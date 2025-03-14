package com.dylanlxlx.instameasure.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * AR测量结果模型
 * 保存测量点、距离和面积信息
 */
public class ArMeasurementResult {
    private List<MeasurementPoint> points;  // 测量点列表
    private List<Float> distances;          // 线段距离列表
    private float totalDistance;            // 总距离
    private float area;                     // 面积
    private boolean isClosed;               // 是否闭合
    private Date timestamp;                 // 测量时间
    private String name;                    // 测量名称

    public ArMeasurementResult() {
        this.points = new ArrayList<>();
        this.distances = new ArrayList<>();
        this.totalDistance = 0f;
        this.area = 0f;
        this.isClosed = false;
        this.timestamp = new Date();
        this.name = "AR测量 " + timestamp.getTime();
    }

    /**
     * 添加测量点
     * @param point 新的测量点
     */
    public void addPoint(MeasurementPoint point) {
        // 如果已经闭合，不再添加点
        if (isClosed) return;

        points.add(point);

        // 如果有两个及以上的点，计算新距离
        if (points.size() > 1) {
            float newDistance = calculateDistance(
                    points.get(points.size() - 2),
                    points.get(points.size() - 1)
            );

            distances.add(newDistance);
            totalDistance += newDistance;
        }
    }

    /**
     * 闭合测量区域
     */
    public void closeArea() {
        if (points.size() < 3 || isClosed) return;

        // 计算从最后一点到第一点的距离
        float closingDistance = calculateDistance(
                points.get(points.size() - 1),
                points.get(0)
        );

        distances.add(closingDistance);
        totalDistance += closingDistance;
        isClosed = true;

        // 计算面积
        calculateArea();
    }

    /**
     * 尝试自动闭合
     * @param threshold 闭合阈值(米)
     * @return 是否成功闭合
     */
    public boolean tryAutoClose(float threshold) {
        if (points.size() < 3 || isClosed) return false;

        MeasurementPoint firstPoint = points.get(0);
        MeasurementPoint lastPoint = points.get(points.size() - 1);

        float distance = calculateDistance(firstPoint, lastPoint);

        if (distance <= threshold) {
            closeArea();
            return true;
        }

        return false;
    }

    /**
     * 计算两个测量点之间的3D距离
     */
    private float calculateDistance(MeasurementPoint p1, MeasurementPoint p2) {
        float[] pos1 = p1.getPosition();
        float[] pos2 = p2.getPosition();

        return (float) Math.sqrt(
                Math.pow(pos2[0] - pos1[0], 2) +
                        Math.pow(pos2[1] - pos1[1], 2) +
                        Math.pow(pos2[2] - pos1[2], 2)
        );
    }

    /**
     * 计算3D多边形面积
     */
    private void calculateArea() {
        if (points.size() < 3) return;

        // 获取所有点的3D坐标
        List<float[]> coords = new ArrayList<>();
        for (MeasurementPoint point : points) {
            coords.add(point.getPosition());
        }

        // 找到最佳投影平面
        // 这里简化为假设XZ平面(地面平面)
        float area = 0;

        for (int i = 0; i < points.size(); i++) {
            float[] p1 = points.get(i).getPosition();
            float[] p2 = points.get((i + 1) % points.size()).getPosition();

            // 使用鞋带公式
            area += (p1[0] * p2[2] - p2[0] * p1[2]);
        }

        this.area = Math.abs(area) / 2.0f;
    }

    /**
     * 重置测量结果
     */
    public void reset() {
        // 清理所有AR资源
        for (MeasurementPoint point : points) {
            point.detach();
        }

        points.clear();
        distances.clear();
        totalDistance = 0f;
        area = 0f;
        isClosed = false;
        timestamp = new Date();
    }

    /**
     * 转换为应用通用的Measurement对象
     */
    public Measurement toMeasurement() {
        List<TrajectoryPoint> trajectoryPoints = new ArrayList<>();

        // 将3D点转换为2D轨迹点
        for (MeasurementPoint point : points) {
            float[] pos = point.getPosition();
            // 使用x,z坐标作为平面坐标
            trajectoryPoints.add(new TrajectoryPoint(pos[0], pos[2]));
        }

        // 如果已闭合，确保轨迹也闭合
        if (isClosed && !trajectoryPoints.isEmpty()) {
            TrajectoryPoint first = trajectoryPoints.get(0);
            trajectoryPoints.add(new TrajectoryPoint(first.getX(), first.getY()));
        }

        return new Measurement(
                name,
                area,
                trajectoryPoints,
                0  // 无步数
        );
    }

    // Getters and Setters
    public List<MeasurementPoint> getPoints() {
        return points;
    }

    public List<Float> getDistances() {
        return distances;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public float getArea() {
        return area;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}