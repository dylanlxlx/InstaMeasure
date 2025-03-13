package com.dylanlxlx.instameasure.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.utils.LocationFusionFilter;
import com.dylanlxlx.instameasure.utils.TrajectoryOptimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * 位置和轨迹数据存储库
 * 管理面积测量的轨迹点
 */
public class LocationRepository {
    private static volatile LocationRepository instance;

    // 轨迹点LiveData
    private final MutableLiveData<List<TrajectoryPoint>> trajectoryPoints = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TrajectoryPoint>> optimizedTrajectoryPoints = new MutableLiveData<>(new ArrayList<>());

    // 当前位置跟踪
    private double currentX = 0.0;
    private double currentY = 0.0;

    // 定位精度(米)
    private final MutableLiveData<Double> locationAccuracy = new MutableLiveData<>(0.0);

    // 定位状态
    private final MutableLiveData<String> locatingMode = new MutableLiveData<>("PDR");

    // 位置融合滤波器
    private final LocationFusionFilter fusionFilter = new LocationFusionFilter();

    // 轨迹优化器
    private final TrajectoryOptimizer trajectoryOptimizer = new TrajectoryOptimizer();

    // 上次轨迹点更新时间
    private long lastTrajectoryUpdateTime = 0;

    // 轨迹点间隔距离(米)
    private static final double TRAJECTORY_POINT_MIN_DISTANCE = 0.3;

    // 上次轨迹点
    private TrajectoryPoint lastTrajectoryPoint = null;

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
     * @param x X坐标(East)
     * @param y Y坐标(North)
     */
    public void addTrajectoryPoint(double x, double y) {
        List<TrajectoryPoint> points = trajectoryPoints.getValue();
        if (points == null) {
            points = new ArrayList<>();
        }

        // 检查与上个点的距离，避免过密点
        if (lastTrajectoryPoint != null) {
            double distance = Math.sqrt(
                    Math.pow(x - lastTrajectoryPoint.getX(), 2) +
                            Math.pow(y - lastTrajectoryPoint.getY(), 2)
            );

            // 如果距离太小且点数已经很多，跳过此点
            if (distance < TRAJECTORY_POINT_MIN_DISTANCE && points.size() > 10) {
                return;
            }
        }

        // 创建新轨迹点
        TrajectoryPoint newPoint = new TrajectoryPoint(x, y);
        points.add(newPoint);
        lastTrajectoryPoint = newPoint;

        // 更新轨迹点列表
        trajectoryPoints.postValue(new ArrayList<>(points));

        // 优化轨迹点
        updateOptimizedTrajectory(points);

        // 更新当前位置
        currentX = x;
        currentY = y;

        // 更新轨迹点更新时间
        lastTrajectoryUpdateTime = System.currentTimeMillis();
    }

    /**
     * 根据步长和方向添加相对位置
     * @param stepLength 步长(米)
     * @param orientation 方向(度，0=北，90=东)
     */
    public void addRelativePosition(float stepLength, float orientation) {
        // 使用融合滤波器更新位置
        fusionFilter.updateWithPdr(stepLength, orientation, 0.5);

        // 获取更新后的位置
        double[] position = fusionFilter.getPosition();

        // 获取位置精度
        double[] accuracy = fusionFilter.getAccuracy();
        locationAccuracy.postValue((accuracy[0] + accuracy[1]) / 2.0);

        // 更新当前位置
        currentX = position[0];
        currentY = position[1];

        // 添加轨迹点
        addTrajectoryPoint(currentX, currentY);

        // 设置定位模式
        locatingMode.postValue("PDR");
    }

    /**
     * 使用GPS数据更新位置
     * @param gpsX GPS东向坐标(米)
     * @param gpsY GPS北向坐标(米)
     * @param accuracy GPS精度(米)
     * @param speed GPS速度(米/秒)
     * @param bearing GPS方位角(度)
     */
    public void updateWithGps(double gpsX, double gpsY, double accuracy, double speed, double bearing) {
        // 使用融合滤波器更新位置
        fusionFilter.updateWithGps(gpsX, gpsY, accuracy, speed, bearing);

        // 获取更新后的位置
        double[] position = fusionFilter.getPosition();

        // 获取位置精度
        double[] accuracyValues = fusionFilter.getAccuracy();
        locationAccuracy.postValue((accuracyValues[0] + accuracyValues[1]) / 2.0);

        // 更新当前位置
        currentX = position[0];
        currentY = position[1];

        // 添加轨迹点(如果间隔足够)
        long now = System.currentTimeMillis();
        if (now - lastTrajectoryUpdateTime > 1000) {  // 至少1秒间隔
            addTrajectoryPoint(currentX, currentY);
        }

        // 设置定位模式
        locatingMode.postValue("Hybrid");
    }

    /**
     * 优化并更新轨迹
     */
    private void updateOptimizedTrajectory(List<TrajectoryPoint> points) {
        // 如果点数较少，不需要优化
        if (points.size() < 10) {
            optimizedTrajectoryPoints.postValue(new ArrayList<>(points));
            return;
        }

        // 使用轨迹优化器简化轨迹
        List<TrajectoryPoint> optimized = trajectoryOptimizer.optimizeTrajectory(points);

        // 更新优化后的轨迹点
        optimizedTrajectoryPoints.postValue(optimized);
    }

    /**
     * 清除所有轨迹点并重置位置
     */
    public void clearTrajectoryPoints() {
        trajectoryPoints.postValue(new ArrayList<>());
        optimizedTrajectoryPoints.postValue(new ArrayList<>());
        currentX = 0.0;
        currentY = 0.0;
        lastTrajectoryPoint = null;
        lastTrajectoryUpdateTime = 0;
        fusionFilter.reset();
        locationAccuracy.postValue(0.0);
        locatingMode.postValue("PDR");
    }

    /**
     * 获取轨迹点的LiveData
     * @return 轨迹点列表的LiveData
     */
    public LiveData<List<TrajectoryPoint>> getTrajectoryPoints() {
        return optimizedTrajectoryPoints; // 返回优化后的轨迹
    }

    /**
     * 获取原始轨迹点的LiveData
     * @return 原始轨迹点列表的LiveData
     */
    public LiveData<List<TrajectoryPoint>> getRawTrajectoryPoints() {
        return trajectoryPoints;
    }

    /**
     * 获取定位精度LiveData
     * @return 定位精度LiveData
     */
    public LiveData<Double> getLocationAccuracy() {
        return locationAccuracy;
    }

    /**
     * 获取定位模式LiveData
     * @return 定位模式LiveData
     */
    public LiveData<String> getLocatingMode() {
        return locatingMode;
    }

    /**
     * 检查轨迹是否封闭
     * @param threshold 封闭阈值(米)
     * @return 是否封闭
     */
    public boolean isTrajectoryEnclosed(double threshold) {
        List<TrajectoryPoint> points = trajectoryPoints.getValue();
        if (points == null || points.size() < 3) {
            return false;
        }

        TrajectoryPoint first = points.get(0);
        TrajectoryPoint last = points.get(points.size() - 1);

        double distance = Math.sqrt(
                Math.pow(last.getX() - first.getX(), 2) +
                        Math.pow(last.getY() - first.getY(), 2)
        );

        return distance <= threshold;
    }

    /**
     * 闭合轨迹(添加起点副本到终点)
     */
    public void closeTrajectory() {
        List<TrajectoryPoint> points = trajectoryPoints.getValue();
        if (points == null || points.size() < 3) {
            return;
        }

        TrajectoryPoint first = points.get(0);
        points.add(new TrajectoryPoint(first.getX(), first.getY()));

        trajectoryPoints.postValue(new ArrayList<>(points));
        updateOptimizedTrajectory(points);
    }
}