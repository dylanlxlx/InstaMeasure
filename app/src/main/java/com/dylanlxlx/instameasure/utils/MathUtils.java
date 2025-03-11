package com.dylanlxlx.instameasure.utils;

// 几何计算工具

import com.dylanlxlx.instameasure.model.TrajectoryPoint;

import java.util.List;

public class MathUtils {
    //TODO:目前仅仅实现了计算规则矩形面积的方法，后续可以继续添加其他不规则几何计算方法

    /**
     * Shoelace Theorem<br><pre>
     * 鞋带定理，也称为高斯面积公式<br>
     * 适用于简单多边形（不交叉的多边形）<br>
     * 计算公式:<br>
     * <img src="https://raw.githubusercontent.com/dylanlxlx/ImageRepository/master/InstaMeasure/formula1.png">
     *
     * @param points 多边形的各个顶点
     * @return 多边形面积
     */
    public static double calculatePolygonArea(List<TrajectoryPoint> points) {
        double area = 0.0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            TrajectoryPoint p1 = points.get(i);
            TrajectoryPoint p2 = points.get((i + 1) % n);
            area += p1.getX() * p2.getY() - p2.getX() * p1.getY();
        }
        return Math.abs(area) / 2.0;
    }

    // 计算三维加速度向量模长
    public static float calculateMagnitude(float[] accelerometer) {
        return (float) Math.sqrt(
                accelerometer[0] * accelerometer[0] +
                        accelerometer[1] * accelerometer[1] +
                        accelerometer[2] * accelerometer[2]
        );
    }

    public static float calculateStandardDeviation(List<Float> accelList) {
        float mean = 0;
        for (float accel : accelList) {
            mean += accel;
        }
        mean /= accelList.size();

        float sumSquaredDiffs = 0;
        for (float accel : accelList) {
            sumSquaredDiffs += (accel - mean) * (accel - mean);
        }

        return (float) Math.sqrt(sumSquaredDiffs / accelList.size());
    }

    // 计算自相关系数
    public static float calculateAutoCorrelation(List<Float> accelList) {
        float mean = 0;
        for (float accel : accelList) {
            mean += accel;
        }
        mean /= accelList.size();

        float numerator = 0;
        float denominator = 0;
        for (int i = 0; i < accelList.size() - 1; i++) {
            numerator += (accelList.get(i) - mean) * (accelList.get(i + 1) - mean);
            denominator += (accelList.get(i) - mean) * (accelList.get(i) - mean);
        }

        return numerator / denominator;
    }
}
