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
public class SensorFilter {
    // 低通滤波：消除高频噪声
    public static float[] lowPass(float[] input, float[] prevOutput, float alpha) {
        if (prevOutput == null) return input;
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = prevOutput[i] + alpha * (input[i] - prevOutput[i]);
        }
        return output;
    }

    // 计算三维加速度向量模长
    public static float calculateMagnitude(float[] accelerometer) {
        return (float) Math.sqrt(
                accelerometer[0] * accelerometer[0] +
                        accelerometer[1] * accelerometer[1] +
                        accelerometer[2] * accelerometer[2]
        );
    }
}
