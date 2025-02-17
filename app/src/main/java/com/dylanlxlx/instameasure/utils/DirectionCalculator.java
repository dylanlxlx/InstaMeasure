package com.dylanlxlx.instameasure.utils;


import android.hardware.SensorManager;

public class DirectionCalculator {
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    // 计算方位角（0=北，90=东，单位：度）
    public float calculateAzimuth(float[] accelerometer, float[] magnetometer) {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometer, magnetometer)) {
            SensorManager.getOrientation(rotationMatrix, orientation);
            return (float) Math.toDegrees(orientation[0]);
        }
        return 0f;
    }
}
