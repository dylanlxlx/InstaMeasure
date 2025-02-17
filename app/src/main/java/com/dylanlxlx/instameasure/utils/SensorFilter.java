package com.dylanlxlx.instameasure.utils;

// 传感器滤波算法
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

    // 计算加速度模长
    public static float calculateMagnitude(float[] accelerometer) {
        return (float) Math.sqrt(
                accelerometer[0] * accelerometer[0] +
                        accelerometer[1] * accelerometer[1] +
                        accelerometer[2] * accelerometer[2]
        );
    }
}
