package com.dylanlxlx.instameasure.model;

public class TrajectoryPoint {
    private final double x; // 东向位移（米）
    private final double y; // 北向位移（米）

    public TrajectoryPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
}