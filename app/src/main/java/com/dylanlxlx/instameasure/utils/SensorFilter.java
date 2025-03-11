package com.dylanlxlx.instameasure.utils;


/**
 * 传感器滤波算法:<br>
 * <pre>Y(n)=αX(n) + (1-α)Y(n-1)<br>
 *   =Y(n-1) + α(X(n)-Y(n-1))<br>
 * Y(n)：本次滤波输出<br>
 * X(n)：本次采样输入<br>
 * Y(n-1)：上次滤波输出<br>
 * α：滤波系数，取值范围[0,1]
 */
//public class SensorFilter {
//    // 低通滤波：消除高频噪声
//    public static float[] lowPass(float[] input, float[] prevOutput, float alpha) {
//        if (prevOutput == null) return input;
//        float[] output = new float[input.length];
//        for (int i = 0; i < input.length; i++) {
//            output[i] = prevOutput[i] + alpha * (input[i] - prevOutput[i]);
//        }
//        return output;
//    }
//}

public class SensorFilter {
    private KalmanFilter1D[] accelFilters;  // 加速度计滤波器（X、Y、Z）
    private KalmanFilter1D[] gyroFilters;   // 陀螺仪滤波器（X、Y、Z）
    private KalmanFilter1D[] magFilters;    // 磁力计滤波器（X、Y、Z）

    /**
     * 构造函数，初始化滤波器
     * @param q 过程噪声协方差
     * @param r 测量噪声协方差
     */
    public SensorFilter(double q, double r) {
        accelFilters = new KalmanFilter1D[3];
        gyroFilters = new KalmanFilter1D[3];
        magFilters = new KalmanFilter1D[3];

        // 为每个轴初始化滤波器，初始状态为0，初始协方差为1
        for (int i = 0; i < 3; i++) {
            accelFilters[i] = new KalmanFilter1D(0, 1, q, r);
            gyroFilters[i] = new KalmanFilter1D(0, 1, q, r);
            magFilters[i] = new KalmanFilter1D(0, 1, q, r);
        }
    }

    /**
     * 滤波加速度计数据
     * @param accelData 原始加速度数据（3个轴）
     * @return 滤波后的加速度数据
     */
    public float[] filterAccelerometer(float[] accelData) {
        float[] filtered = new float[3];
        for (int i = 0; i < 3; i++) {
            filtered[i] = (float) accelFilters[i].filter(accelData[i]);
        }
        return filtered;
    }

    /**
     * 滤波陀螺仪数据
     * @param gyroData 原始陀螺仪数据（3个轴）
     * @return 滤波后的陀螺仪数据
     */
    public float[] filterGyroscope(float[] gyroData) {
        float[] filtered = new float[3];
        for (int i = 0; i < 3; i++) {
            filtered[i] = (float) gyroFilters[i].filter(gyroData[i]);
        }
        return filtered;
    }

    /**
     * 滤波磁力计数据
     * @param magData 原始磁力计数据（3个轴）
     * @return 滤波后的磁力计数据
     */
    public float[] filterMagneticField(float[] magData) {
        float[] filtered = new float[3];
        for (int i = 0; i < 3; i++) {
            filtered[i] = (float) magFilters[i].filter(magData[i]);
        }
        return filtered;
    }
}
