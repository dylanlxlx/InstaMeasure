package com.dylanlxlx.instameasure.utils;

// 几何计算工具
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import java.util.List;

public class MathUtils {
    //TODO:目前仅仅实现了计算规则矩形面积的方法，后续可以继续添加其他不规则几何计算方法
    public static double calculatePolygonArea(List<TrajectoryPoint> points) {
        double area = 0.0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            TrajectoryPoint p1 = points.get(i);
            TrajectoryPoint p2 = points.get((i+1)%n);
            area += p1.getX() * p2.getY() - p2.getX() * p1.getY();
        }
        return Math.abs(area) / 2.0;
    }
}
