package com.dylanlxlx.instameasure.utils;

import com.dylanlxlx.instameasure.model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 轨迹优化器
 * 使用Ramer-Douglas-Peucker算法简化轨迹
 */
public class TrajectoryOptimizer {
    // 优化参数
    private static final double DISTANCE_THRESHOLD = 0.5;  // 距离阈值(米)
    private static final double ANGLE_THRESHOLD = 15.0;    // 角度阈值(度)
    private static final int MAX_POINTS = 1000;            // 最大轨迹点数

    /**
     * 简化轨迹，去除冗余点
     * @param points 原始轨迹点
     * @return 简化后的轨迹点
     */
    public List<TrajectoryPoint> optimizeTrajectory(List<TrajectoryPoint> points) {
        if (points == null || points.size() <= 2) {
            return new ArrayList<>(points != null ? points : new ArrayList<>());
        }

        // 如果点数过多，先进行降采样
        if (points.size() > MAX_POINTS) {
            points = downsample(points, MAX_POINTS);
        }

        // 使用Ramer-Douglas-Peucker算法简化轨迹
        List<TrajectoryPoint> result = new ArrayList<>();
        boolean[] keepPoint = new boolean[points.size()];

        // 标记首尾点为保留
        keepPoint[0] = true;
        keepPoint[points.size() - 1] = true;

        // 运行RDP算法
        rdpSimplify(points, 0, points.size() - 1, DISTANCE_THRESHOLD, keepPoint);

        // 收集要保留的点
        for (int i = 0; i < points.size(); i++) {
            if (keepPoint[i]) {
                result.add(points.get(i));
            }
        }

        return result;
    }

    /**
     * Ramer-Douglas-Peucker算法实现
     */
    private void rdpSimplify(List<TrajectoryPoint> points, int start, int end,
                             double epsilon, boolean[] keepPoint) {
        if (end <= start + 1) {
            return; // 递归终止条件
        }

        // 找到离线段最远的点
        double maxDistance = 0;
        int farthestIndex = start;

        TrajectoryPoint startPoint = points.get(start);
        TrajectoryPoint endPoint = points.get(end);

        for (int i = start + 1; i < end; i++) {
            double distance = perpendicularDistance(points.get(i), startPoint, endPoint);
            if (distance > maxDistance) {
                maxDistance = distance;
                farthestIndex = i;
            }
        }

        // 如果最远点的距离大于epsilon，则保留该点并递归两边
        if (maxDistance > epsilon) {
            keepPoint[farthestIndex] = true;

            // 递归处理两个子段
            rdpSimplify(points, start, farthestIndex, epsilon, keepPoint);
            rdpSimplify(points, farthestIndex, end, epsilon, keepPoint);
        }
    }

    /**
     * 计算点到线段的垂直距离
     */
    private double perpendicularDistance(TrajectoryPoint point,
                                         TrajectoryPoint lineStart,
                                         TrajectoryPoint lineEnd) {
        double x = point.getX(), y = point.getY();
        double x1 = lineStart.getX(), y1 = lineStart.getY();
        double x2 = lineEnd.getX(), y2 = lineEnd.getY();

        // 线段长度的平方
        double lineLength2 = (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);

        if (lineLength2 == 0) {
            // 如果起点和终点重合，则计算点到点的距离
            return Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
        }

        // 计算点在线段上的投影位置参数t
        double t = ((x-x1)*(x2-x1) + (y-y1)*(y2-y1)) / lineLength2;

        // 约束t在[0,1]范围内，表示点在线段上的投影
        t = Math.max(0, Math.min(1, t));

        // 计算投影点坐标
        double projectionX = x1 + t * (x2-x1);
        double projectionY = y1 + t * (y2-y1);

        // 计算点到投影点的距离
        return Math.sqrt((x-projectionX)*(x-projectionX) + (y-projectionY)*(y-projectionY));
    }

    /**
     * 降采样轨迹点
     * @param points 原始点
     * @param targetCount 目标点数
     * @return 降采样后的点
     */
    private List<TrajectoryPoint> downsample(List<TrajectoryPoint> points, int targetCount) {
        if (points.size() <= targetCount) {
            return new ArrayList<>(points);
        }

        List<TrajectoryPoint> result = new ArrayList<>();

        // 始终保留首尾点
        result.add(points.get(0));

        // 计算采样间隔
        double step = (points.size() - 2) / (double)(targetCount - 2);

        // 采样中间点
        for (int i = 1; i < targetCount - 1; i++) {
            int index = (int)Math.floor(i * step) + 1;
            result.add(points.get(index));
        }

        // 添加最后一个点
        result.add(points.get(points.size() - 1));

        return result;
    }

    /**
     * 检测并闭合轨迹(如果起点和终点接近)
     * @param points 轨迹点
     * @param threshold 闭合阈值(米)
     * @return 处理后的轨迹点
     */
    public List<TrajectoryPoint> closeTrajectoryIfNeeded(List<TrajectoryPoint> points, double threshold) {
        if (points == null || points.size() < 3) {
            return points;
        }

        TrajectoryPoint first = points.get(0);
        TrajectoryPoint last = points.get(points.size() - 1);

        // 计算起点和终点距离
        double distance = Math.sqrt(
                Math.pow(last.getX() - first.getX(), 2) +
                        Math.pow(last.getY() - first.getY(), 2)
        );

        // 如果距离小于阈值，添加起点副本作为终点以闭合轨迹
        if (distance <= threshold) {
            List<TrajectoryPoint> closedTrajectory = new ArrayList<>(points);
            closedTrajectory.add(new TrajectoryPoint(first.getX(), first.getY()));
            return closedTrajectory;
        }

        return points;
    }
}