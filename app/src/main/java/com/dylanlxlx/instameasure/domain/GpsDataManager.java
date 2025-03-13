package com.dylanlxlx.instameasure.domain;

import android.location.Location;

import com.dylanlxlx.instameasure.model.GpsData;
import com.dylanlxlx.instameasure.utils.KalmanFilter1D;

/**
 * GPS数据管理器
 * 负责GPS数据的处理、滤波与融合
 */
public class GpsDataManager {

    // 接口：GPS数据处理回调
    public interface GpsDataCallback {
        void onGpsDataProcessed(GpsData gpsData);
        void onGpsUnavailable();
    }

    private final GpsDataCallback callback;

    // 卡尔曼滤波器（用于滤波经纬度）
    private final KalmanFilter1D latitudeFilter;
    private final KalmanFilter1D longitudeFilter;

    // 最近的有效GPS数据
    private GpsData lastValidGpsData = null;

    // 精度阈值（米），超过此值的GPS点被视为不可靠
    private static final float ACCURACY_THRESHOLD = 20.0f;

    // 速度异常阈值（米/秒），超过此值的GPS点被视为异常
    private static final float SPEED_THRESHOLD = 10.0f;

    /**
     * 构造函数
     * @param callback GPS数据处理回调
     */
    public GpsDataManager(GpsDataCallback callback) {
        this.callback = callback;

        // 初始化卡尔曼滤波器
        // 参数: 初始状态、初始协方差、过程噪声协方差、测量噪声协方差
        this.latitudeFilter = new KalmanFilter1D(0, 1, 0.00001, 0.001);
        this.longitudeFilter = new KalmanFilter1D(0, 1, 0.00001, 0.001);
    }

    /**
     * 处理新的GPS位置
     * @param location GPS位置
     */
    public void processGpsLocation(Location location) {
        if (location == null) {
            if (callback != null) {
                callback.onGpsUnavailable();
            }
            return;
        }

        // 检查GPS数据的有效性
        if (!isValidLocation(location)) {
            if (callback != null) {
                callback.onGpsUnavailable();
            }
            return;
        }

        // 应用卡尔曼滤波
        double filteredLat = latitudeFilter.filter(location.getLatitude());
        double filteredLon = longitudeFilter.filter(location.getLongitude());

        // 创建GpsData对象
        GpsData gpsData = new GpsData(
                filteredLat,
                filteredLon,
                location.getAltitude(),
                location.getAccuracy(),
                location.getSpeed(),
                location.getBearing(),
                location.getTime(),
                location.getExtras() != null ? location.getExtras().getInt("satellites", 0) : 0
        );

        // 更新最近的有效GPS数据
        lastValidGpsData = gpsData;

        // 通过回调传递处理后的GPS数据
        if (callback != null) {
            callback.onGpsDataProcessed(gpsData);
        }
    }

    /**
     * 检查位置数据的有效性
     * @param location 位置数据
     * @return 是否有效
     */
    private boolean isValidLocation(Location location) {
        // 检查精度
        if (location.getAccuracy() > ACCURACY_THRESHOLD) {
            return false;
        }

        // 检查速度异常
        if (location.hasSpeed() && location.getSpeed() > SPEED_THRESHOLD) {
            return false;
        }

        // 检查与上一个点的合理性
        if (lastValidGpsData != null) {
            double distance = com.dylanlxlx.instameasure.utils.LocationUtils.calculateDistance(
                    lastValidGpsData.getLatitude(), lastValidGpsData.getLongitude(),
                    location.getLatitude(), location.getLongitude());
            long timeDiff = location.getTime() - lastValidGpsData.getTimestamp();

            // 如果时间差太小但距离很大，可能是异常点
            if (timeDiff < 1000 && distance > 10) {
                return false;
            }

            // 如果计算出的速度不合理，可能是异常点
            double speed = distance / (timeDiff / 1000.0);
            if (speed > SPEED_THRESHOLD) {
                return false;
            }
        }

        return true;
    }

    /**
     * 重置滤波器
     */
    public void resetFilters() {
        // 重新初始化滤波器
        lastValidGpsData = null;
    }

    /**
     * 获取最后一个有效的GPS数据
     */
    public GpsData getLastValidGpsData() {
        return lastValidGpsData;
    }
}