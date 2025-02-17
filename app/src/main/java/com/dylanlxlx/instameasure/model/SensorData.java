package com.dylanlxlx.instameasure.model;

// 封装加速度、陀螺仪等传感器数据
public class SensorData {
    private final float[] accelerometer; // 加速度 [x,y,z]
    private final float[] gyroscope;     // 陀螺仪 [x,y,z]
    private final float[] magnetometer;  // 磁力计 [x,y,z]
    private final long timestamp;       // 时间戳

    public SensorData(float[] accelerometer, float[] gyroscope, float[] magnetometer, long timestamp) {
        this.accelerometer = accelerometer;
        this.gyroscope = gyroscope;
        this.magnetometer = magnetometer;
        this.timestamp = timestamp;
    }

    // Getters
    public float[] getAccelerometer() { return accelerometer; }
    public float[] getGyroscope() { return gyroscope; }
    public float[] getMagnetometer() { return magnetometer; }
    public long getTimestamp() { return timestamp; }
}
