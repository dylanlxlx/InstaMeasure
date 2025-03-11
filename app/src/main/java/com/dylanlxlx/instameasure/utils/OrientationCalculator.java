package com.dylanlxlx.instameasure.utils;


import android.hardware.SensorManager;

public class OrientationCalculator {
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    // 计算方位角（0=北，90=东，单位：度）
    public float calculateAzimuth(float[] accelerometer, float[] magnetometer) {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometer, magnetometer)) {
            SensorManager.getOrientation(rotationMatrix, orientation);
            int degree = (int) Math.toDegrees(orientation[0]);//旋转角度
            return degree < 0 ? degree + 360f : degree;
        }
        return 0f;
    }
}
