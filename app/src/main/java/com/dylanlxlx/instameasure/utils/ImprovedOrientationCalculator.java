package com.dylanlxlx.instameasure.utils;

import android.hardware.SensorManager;

public class ImprovedOrientationCalculator {
    // 旋转矩阵
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    // 陀螺仪积分
    private float[] gyroIntegration = new float[3];
    private long lastTimestamp = 0;

    // 互补滤波参数 (陀螺仪权重)
    private static final float ALPHA = 0.98f;
    // 陀螺仪漂移校正系数
    private static final float GYRO_DRIFT_CORRECTION = 0.01f;

    // 磁场异常检测
    private float[] lastMagneticValues = null;
    private float magneticDisturbanceThreshold = 5.0f;
    private boolean magneticDisturbance = false;

    /**
     * 使用互补滤波计算方位角
     * @param accelerometer 加速度计数据
     * @param magnetometer 磁力计数据
     * @param gyroscope 陀螺仪数据
     * @param timestamp 当前时间戳(毫秒)
     * @return 方位角(0=北，90=东，单位：度)
     */
    public float calculateAzimuth(float[] accelerometer, float[] magnetometer,
                                  float[] gyroscope, long timestamp) {
        // 检测磁场异常
        if (lastMagneticValues != null) {
            float diff = 0;
            for (int i = 0; i < 3; i++) {
                diff += Math.abs(magnetometer[i] - lastMagneticValues[i]);
            }
            magneticDisturbance = diff > magneticDisturbanceThreshold;
        }
        lastMagneticValues = magnetometer.clone();

        // 使用磁力计和加速度计计算方位角
        float magneticAzimuth = 0;
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometer, magnetometer)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            magneticAzimuth = orientationAngles[0];
        }

        // 陀螺仪积分计算方位角变化
        if (lastTimestamp != 0) {
            float dt = (timestamp - lastTimestamp) / 1000.0f; // 转换为秒

            // 陀螺仪Z轴对应方位角变化(绕Z轴旋转)
            gyroIntegration[0] += gyroscope[2] * dt;

            // 应用漂移校正
            if (!magneticDisturbance) {
                // 减少陀螺仪漂移(向磁力计方位靠拢)
                float diff = magneticAzimuth - gyroIntegration[0];
                // 处理角度跨越±π边界
                if (diff > Math.PI) diff -= 2 * Math.PI;
                if (diff < -Math.PI) diff += 2 * Math.PI;

                gyroIntegration[0] += GYRO_DRIFT_CORRECTION * diff;
            }
        }
        lastTimestamp = timestamp;

        // 融合结果
        // 如果磁场异常，增加陀螺仪权重
        float fusionAlpha = magneticDisturbance ? 0.99f : ALPHA;
        float filteredAzimuth = fusionAlpha * gyroIntegration[0] + (1 - fusionAlpha) * magneticAzimuth;

        // 转换为度数并规范化到0-360范围
        float degree = (float) Math.toDegrees(filteredAzimuth);
        return degree < 0 ? degree + 360f : degree;
    }

    /**
     * 当有GPS方位角时校准陀螺仪积分
     * @param gpsBearing GPS方位角(度)
     */
    public void calibrateWithGps(float gpsBearing) {
        float gpsAzimuthRad = (float) Math.toRadians(gpsBearing);

        // 计算差值，处理角度环绕
        float diff = gpsAzimuthRad - gyroIntegration[0];
        if (diff > Math.PI) diff -= 2 * Math.PI;
        if (diff < -Math.PI) diff += 2 * Math.PI;

        // 软校准，避免突变
        if (Math.abs(diff) < Math.PI / 4) { // 小于45度才校准，防止错误方位
            gyroIntegration[0] += diff * 0.3f; // 逐渐校准
        }
    }

    /**
     * 重置方位计算状态
     */
    public void reset() {
        lastTimestamp = 0;
        gyroIntegration = new float[3];
        lastMagneticValues = null;
        magneticDisturbance = false;
    }
}