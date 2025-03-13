package com.dylanlxlx.instameasure.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 位置和轨迹数据的存储库。
 * 管理面积测量的轨迹点。
 */
public class LocationRepository {
    private static volatile LocationRepository instance;

    // 轨迹点的 LiveData
    private final MutableLiveData<List<TrajectoryPoint>> trajectoryPoints = new MutableLiveData<>(new ArrayList<>());

    // 当前位置跟踪
    private double currentX = 0.0;
    private double currentY = 0.0;

    private LocationRepository() {
    }

    public static LocationRepository getInstance() {
        if (instance == null) {
            synchronized (LocationRepository.class) {
                if (instance == null) {
                    instance = new LocationRepository();
                }
            }
        }
        return instance;
    }

    /**
     * 根据绝对坐标添加轨迹点
     * @param x X 坐标 (East)
     * @param y Y 坐标 (North)
     */
    public void addTrajectoryPoint(double x, double y) {
        List<TrajectoryPoint> points = trajectoryPoints.getValue();
        if (points == null) {
            points = new ArrayList<>();
        }

        points.add(new TrajectoryPoint(x, y));
        trajectoryPoints.postValue(new ArrayList<>(points)); // 创建新列表以触发观察者

        // 更新当前位置
        currentX = x;
        currentY = y;
    }

    /**
     * 根据步长和方向添加相对位置
     * @param stepLength 步长
     * @param orientation 方向（以度为单位） (0=North, 90=East)
     */
    public void addRelativePosition(float stepLength, float orientation) {
        // Calculate new position (x=East, y=North)
        double deltaX = stepLength * Math.sin(Math.toRadians(orientation));
        double deltaY = stepLength * Math.cos(Math.toRadians(orientation));

        currentX += deltaX;
        currentY += deltaY;

        addTrajectoryPoint(currentX, currentY);
    }

    /**
     * 清除所有轨迹点并重置位置
     */
    public void clearTrajectoryPoints() {
        trajectoryPoints.postValue(new ArrayList<>());
        currentX = 0.0;
        currentY = 0.0;
    }

    /**
     * 获取轨迹点的 LiveData
     * @return 轨迹点列表的 LiveData
     */
    public LiveData<List<TrajectoryPoint>> getTrajectoryPoints() {
        return trajectoryPoints;
    }
}