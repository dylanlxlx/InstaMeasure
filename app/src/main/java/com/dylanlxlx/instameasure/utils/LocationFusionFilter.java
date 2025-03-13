package com.dylanlxlx.instameasure.utils;

/**
 * 定位融合滤波器
 * 使用扩展卡尔曼滤波器融合GPS和PDR数据
 */
public class LocationFusionFilter {
    // 状态向量：[x, y, vx, vy, heading]
    private double[] state = new double[5];
    private double[][] covariance = new double[5][5];

    // 系统噪声参数
    private double processNoise = 0.01;        // 系统过程噪声
    private double gpsPositionNoise = 5.0;     // GPS位置噪声(米)
    private double pdrPositionNoise = 0.5;     // PDR位置噪声(米)
    private double headingNoise = 0.1;         // 方向噪声(弧度)

    // 融合控制参数
    private boolean hasGpsFix = false;         // 是否有GPS fix
    private long lastGpsTime = 0;              // 上次GPS更新时间
    private long gpsTimeout = 10000;           // GPS超时时间(毫秒)

    /**
     * 构造函数
     */
    public LocationFusionFilter() {
        // 初始化协方差矩阵
        for (int i = 0; i < 5; i++) {
            covariance[i][i] = i < 2 ? 10.0 : (i < 4 ? 1.0 : 0.5);  // 初始不确定性
        }
    }

    /**
     * 使用PDR数据更新
     * @param stepLength 步长(米)
     * @param heading 方向(度)
     * @param dt 时间间隔(秒)
     */
    public void updateWithPdr(double stepLength, double heading, double dt) {
        double headingRad = Math.toRadians(heading);

        // 状态预测
        // 位置更新(基于速度和步长)
        double dx = stepLength * Math.sin(headingRad);
        double dy = stepLength * Math.cos(headingRad);

        state[0] += state[2] * dt + dx;  // x位置
        state[1] += state[3] * dt + dy;  // y位置

        // 速度更新(放入系统参数以便与GPS融合)
        if (dt > 0) {
            state[2] = 0.8 * state[2] + 0.2 * (dx / dt);  // x速度
            state[3] = 0.8 * state[3] + 0.2 * (dy / dt);  // y速度
        }

        // 方向更新
        state[4] = headingRad;

        // 协方差预测(增加不确定性)
        // P = F*P*F' + Q
        for (int i = 0; i < 2; i++) {
            covariance[i][i] += processNoise + pdrPositionNoise * pdrPositionNoise;
            covariance[i+2][i+2] += processNoise * 2;  // 速度不确定性增加更多
        }
        covariance[4][4] += headingNoise;  // 方向不确定性

        // 检查是否需要降低GPS权重(GPS长时间无更新)
        if (hasGpsFix && System.currentTimeMillis() - lastGpsTime > gpsTimeout) {
            hasGpsFix = false;
        }
    }

    /**
     * 使用GPS数据更新
     * @param gpsX GPS东向坐标(米)
     * @param gpsY GPS北向坐标(米)
     * @param accuracy GPS精度(米)
     * @param speed GPS速度(米/秒)
     * @param bearing GPS方位角(度)
     */
    public void updateWithGps(double gpsX, double gpsY, double accuracy,
                              double speed, double bearing) {
        // 记录GPS更新时间
        lastGpsTime = System.currentTimeMillis();
        hasGpsFix = true;

        // 动态调整GPS噪声(基于精度)
        double currentGpsNoise = accuracy > 0 ? accuracy * accuracy : gpsPositionNoise;

        // 位置更新
        updatePositionWithGps(gpsX, gpsY, currentGpsNoise);

        // 速度和方向更新(仅当GPS速度可用时)
        if (speed > 0.5) {  // 速度大于0.5m/s才可靠
            updateVelocityWithGps(speed, bearing, currentGpsNoise);
        }
    }

    /**
     * 使用GPS更新位置
     */
    private void updatePositionWithGps(double gpsX, double gpsY, double noise) {
        // 计算卡尔曼增益(K = P*H'/(H*P*H' + R))
        double kx = covariance[0][0] / (covariance[0][0] + noise);
        double ky = covariance[1][1] / (covariance[1][1] + noise);

        // 测量残差
        double residualX = gpsX - state[0];
        double residualY = gpsY - state[1];

        // 状态更新
        state[0] += kx * residualX;
        state[1] += ky * residualY;

        // 更新速度(使用残差信息)
        state[2] += (kx * residualX) * 0.1;
        state[3] += (ky * residualY) * 0.1;

        // 更新协方差
        covariance[0][0] *= (1 - kx);
        covariance[1][1] *= (1 - ky);
    }

    /**
     * 使用GPS更新速度和方向
     */
    private void updateVelocityWithGps(double speed, double bearing, double noise) {
        // 转换方位角到弧度
        double bearingRad = Math.toRadians(bearing);

        // 计算GPS速度分量
        double gpsVx = speed * Math.sin(bearingRad);
        double gpsVy = speed * Math.cos(bearingRad);

        // 速度噪声(基于GPS精度调整)
        double velocityNoise = noise * 0.1;

        // 计算卡尔曼增益
        double kvx = covariance[2][2] / (covariance[2][2] + velocityNoise);
        double kvy = covariance[3][3] / (covariance[3][3] + velocityNoise);

        // 更新速度
        state[2] += kvx * (gpsVx - state[2]);
        state[3] += kvy * (gpsVy - state[3]);

        // 更新协方差
        covariance[2][2] *= (1 - kvx);
        covariance[3][3] *= (1 - kvy);

        // 更新方向(仅当速度足够大时)
        if (speed > 1.0) {
            // 计算的方向(从速度分量)
            double calculatedHeading = Math.atan2(gpsVx, gpsVy);

            // 计算卡尔曼增益
            double kh = covariance[4][4] / (covariance[4][4] + headingNoise);

            // 计算角度差，处理±π边界
            double headingDiff = calculatedHeading - state[4];
            if (headingDiff > Math.PI) headingDiff -= 2 * Math.PI;
            if (headingDiff < -Math.PI) headingDiff += 2 * Math.PI;

            // 更新方向
            state[4] += kh * headingDiff;

            // 规范化方向到[0, 2π)
            while (state[4] < 0) state[4] += 2 * Math.PI;
            while (state[4] >= 2 * Math.PI) state[4] -= 2 * Math.PI;

            // 更新协方差
            covariance[4][4] *= (1 - kh);
        }
    }

    /**
     * 获取当前位置
     * @return [x, y] 位置(米)
     */
    public double[] getPosition() {
        return new double[]{state[0], state[1]};
    }

    /**
     * 获取当前速度
     * @return [vx, vy] 速度(米/秒)
     */
    public double[] getVelocity() {
        return new double[]{state[2], state[3]};
    }

    /**
     * 获取当前方向
     * @return 方向(度，0=北，90=东)
     */
    public double getHeading() {
        return Math.toDegrees(state[4]);
    }

    /**
     * 获取位置精度(不确定性)
     * @return [水平精度, 垂直精度] (米)
     */
    public double[] getAccuracy() {
        return new double[]{
                Math.sqrt(covariance[0][0]),
                Math.sqrt(covariance[1][1])
        };
    }

    /**
     * 重置滤波器状态
     */
    public void reset() {
        // 清零状态向量
        for (int i = 0; i < state.length; i++) {
            state[i] = 0;
        }

        // 重置协方差
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                covariance[i][j] = 0;
            }
            covariance[i][i] = i < 2 ? 10.0 : (i < 4 ? 1.0 : 0.5);
        }

        hasGpsFix = false;
        lastGpsTime = 0;
    }
}