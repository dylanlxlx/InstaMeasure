package com.dylanlxlx.instameasure.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.model.GpsData;
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.utils.LocationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GPS数据仓库
 * 管理GPS定位数据及轨迹
 */
public class GpsRepository {
    private static volatile GpsRepository instance;

    // LiveData对象
    private final MutableLiveData<GpsData> currentGpsData = new MutableLiveData<>();
    private final MutableLiveData<List<TrajectoryPoint>> gpsTrajectoryPoints = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Float> gpsAccuracy = new MutableLiveData<>(0f);
    private final MutableLiveData<Integer> satelliteCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isGpsAvailable = new MutableLiveData<>(false);

    // 最近的GPS轨迹点
    private double lastLatitude = 0;
    private double lastLongitude = 0;

    // 本地坐标系原点（首个GPS点作为原点）
    private double originLatitude = 0;
    private double originLongitude = 0;
    private boolean hasOrigin = false;

    private GpsRepository() {
        // 私有构造函数
    }

    public static GpsRepository getInstance() {
        if (instance == null) {
            synchronized (GpsRepository.class) {
                if (instance == null) {
                    instance = new GpsRepository();
                }
            }
        }
        return instance;
    }

    /**
     * 更新当前GPS数据
     *
     * @param gpsData 新的GPS数据
     */
    public void updateGpsData(GpsData gpsData) {
        currentGpsData.postValue(gpsData);

        // 更新最近的经纬度
        lastLatitude = gpsData.getLatitude();
        lastLongitude = gpsData.getLongitude();

        // 更新GPS精度和可用性
        gpsAccuracy.postValue(gpsData.getAccuracy());
        isGpsAvailable.postValue(true);

        // 更新卫星数量（如果有）
        if (gpsData.getSatelliteCount() > 0) {
            satelliteCount.postValue(gpsData.getSatelliteCount());
        }

        // 处理轨迹点
        processGpsTrajectoryPoint(gpsData);
    }

    /**
     * 处理GPS轨迹点
     *
     * @param gpsData GPS数据
     */
    private void processGpsTrajectoryPoint(GpsData gpsData) {
        // 如果还没有设置原点，则将第一个点设为原点
        if (!hasOrigin) {
            originLatitude = gpsData.getLatitude();
            originLongitude = gpsData.getLongitude();
            hasOrigin = true;
        }

        // 将地理坐标（经纬度）转换为本地坐标（米）
        double[] localCoords = LocationUtils.geoToLocalCoordinates(
                gpsData.getLatitude(), gpsData.getLongitude(),
                originLatitude, originLongitude);

        // 添加轨迹点
        List<TrajectoryPoint> points = gpsTrajectoryPoints.getValue();
        if (points == null) {
            points = new ArrayList<>();
        }

        points.add(new TrajectoryPoint(localCoords[0], localCoords[1]));
        gpsTrajectoryPoints.postValue(new ArrayList<>(points));
    }

    /**
     * 设置GPS不可用
     */
    public void setGpsUnavailable() {
        isGpsAvailable.postValue(false);
    }

    /**
     * 清除GPS轨迹点
     */
    public void clearGpsTrajectoryPoints() {
        gpsTrajectoryPoints.postValue(new ArrayList<>());
        hasOrigin = false;
    }

    /**
     * 获取当前GPS数据
     */
    public LiveData<GpsData> getCurrentGpsData() {
        return currentGpsData;
    }

    /**
     * 获取GPS轨迹点
     */
    public LiveData<List<TrajectoryPoint>> getGpsTrajectoryPoints() {
        return gpsTrajectoryPoints;
    }

    /**
     * 获取GPS精度
     */
    public LiveData<Float> getGpsAccuracy() {
        return gpsAccuracy;
    }

    /**
     * 获取卫星数量
     */
    public LiveData<Integer> getSatelliteCount() {
        return satelliteCount;
    }

    /**
     * 获取GPS可用性
     */
    public LiveData<Boolean> isGpsAvailable() {
        return isGpsAvailable;
    }

    /**
     * 获取最后一次位置的经纬度
     */
    public double[] getLastLocation() {
        return new double[]{lastLatitude, lastLongitude};
    }
}