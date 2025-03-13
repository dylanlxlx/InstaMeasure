package com.dylanlxlx.instameasure.model;

import java.util.Date;
import java.util.List;

/**
 * 表示测量值的 Model 类。
 * 包含与单个测量会话相关的所有数据
 */
public class Measurement {
    private long id;
    private String name;
    private double area;
    private Date timestamp;
    private List<TrajectoryPoint> trajectoryPoints;
    private int stepCount;

    /**
     * 用于创建新测量的构造函数
     * @param name 测量名称/描述
     * @param area 计算面积（平方米）
     * @param trajectoryPoints 轨迹点列表
     * @param stepCount 步数
     */
    public Measurement(String name, double area, List<TrajectoryPoint> trajectoryPoints, int stepCount) {
        this.name = name;
        this.area = area;
        this.trajectoryPoints = trajectoryPoints;
        this.stepCount = stepCount;
        this.timestamp = new Date();
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<TrajectoryPoint> getTrajectoryPoints() {
        return trajectoryPoints;
    }

    public void setTrajectoryPoints(List<TrajectoryPoint> trajectoryPoints) {
        this.trajectoryPoints = trajectoryPoints;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }
}