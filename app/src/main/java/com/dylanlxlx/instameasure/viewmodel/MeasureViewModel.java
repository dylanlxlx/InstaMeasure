package com.dylanlxlx.instameasure.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dylanlxlx.instameasure.data.repository.SensorRepository;
import com.dylanlxlx.instameasure.data.repository.LocationRepository;
import com.dylanlxlx.instameasure.data.repository.MeasurementRepository;
import com.dylanlxlx.instameasure.data.repository.GpsRepository;
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.model.Measurement;
import com.dylanlxlx.instameasure.utils.MathUtils;
import com.dylanlxlx.instameasure.utils.TrajectoryOptimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * 测量功能的ViewModel
 * 处理存储库和UI之间的通信
 */
public class MeasureViewModel extends ViewModel {
    private final SensorRepository sensorRepository;
    private final LocationRepository locationRepository;
    private final MeasurementRepository measurementRepository;
    private final GpsRepository gpsRepository;

    private final MutableLiveData<Double> measuredArea = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isMeasuring = new MutableLiveData<>(false);
    private final MutableLiveData<String> locatingMode = new MutableLiveData<>("PDR");

    private final TrajectoryOptimizer trajectoryOptimizer = new TrajectoryOptimizer();

    // 用户设置
    private float userHeight = 1.7f;  // 默认身高(米)
    private boolean useGps = false;   // 是否使用GPS

    public MeasureViewModel() {
        // 获取存储库实例
        this.sensorRepository = SensorRepository.getInstance();
        this.locationRepository = LocationRepository.getInstance();
        this.measurementRepository = MeasurementRepository.getInstance();
        this.gpsRepository = GpsRepository.getInstance();

        // 监听定位模式变化
        locationRepository.getLocatingMode().observeForever(mode -> {
            locatingMode.setValue(mode);
        });
    }

    /**
     * 开始新的测量会话
     */
    public void startMeasurement() {
        // 清除轨迹数据
        locationRepository.clearTrajectoryPoints();
        gpsRepository.clearGpsTrajectoryPoints();

        // 重置传感器处理器
        sensorRepository.resetSensorProcessors();

        // 添加起点
        locationRepository.addTrajectoryPoint(0.0, 0.0);

        // 设置测量状态
        isMeasuring.setValue(true);

        // 如果使用GPS，初始化GPS位置
        if (useGps) {
            // GPS位置将在服务中获取并更新
        }
    }

    /**
     * 停止当前测量会话
     */
    public void stopMeasurement() {
        isMeasuring.setValue(false);

        // 在计算面积前检查轨迹是否闭合
        if (locationRepository.isTrajectoryEnclosed(2.0)) {
            locationRepository.closeTrajectory();
        }

        calculateArea();
    }

    /**
     * 使用鞋带定理计算当前轨迹的面积
     */
    public void calculateArea() {
        List<TrajectoryPoint> points = locationRepository.getTrajectoryPoints().getValue();
        if (points != null && points.size() > 2) {
            // 确保轨迹闭合
            points = trajectoryOptimizer.closeTrajectoryIfNeeded(points, 2.0);

            // 计算面积
            double area = MathUtils.calculatePolygonArea(points);
            measuredArea.setValue(area);

            // 创建和保存测量数据
            Measurement measurement = new Measurement(
                    "Measurement " + System.currentTimeMillis(),
                    area,
                    new ArrayList<>(points),
                    sensorRepository.getStepCount().getValue() != null ?
                            sensorRepository.getStepCount().getValue() : 0
            );
            measurementRepository.saveMeasurement(measurement);
        }
    }

    /**
     * 根据步长和方向添加相对位置
     */
    public void addPosition(float stepLength, float orientation) {
        if (Boolean.TRUE.equals(isMeasuring.getValue())) {
            locationRepository.addRelativePosition(stepLength, orientation);

            // 如果有GPS数据且启用了GPS，融合GPS位置
            if (useGps && gpsRepository.isGpsAvailable().getValue() == Boolean.TRUE) {
                updateWithGps();
            }
        }
    }

    /**
     * 使用GPS数据更新位置
     */
    private void updateWithGps() {
        // 获取最新GPS数据
        if (gpsRepository.getCurrentGpsData().getValue() != null) {
            double[] localCoords = gpsRepository.getLastLocalCoordinates();
            float accuracy = gpsRepository.getGpsAccuracy().getValue() != null ?
                    gpsRepository.getGpsAccuracy().getValue() : 10.0f;
            float speed = gpsRepository.getCurrentGpsData().getValue().getSpeed();
            float bearing = gpsRepository.getCurrentGpsData().getValue().getBearing();

            // 使用GPS数据更新位置
            locationRepository.updateWithGps(
                    localCoords[0], localCoords[1], accuracy, speed, bearing
            );

            // 校准方向
            sensorRepository.calibrateOrientationWithGps(bearing);
        }
    }

    /**
     * 添加轨迹点
     * @param x X坐标
     * @param y Y坐标
     */
    public void addTrajectoryPoint(double x, double y) {
        if (Boolean.TRUE.equals(isMeasuring.getValue())) {
            locationRepository.addTrajectoryPoint(x, y);
        }
    }

    /**
     * 清除轨迹点并重置位置
     */
    public void clearTrajectoryPoints() {
        locationRepository.clearTrajectoryPoints();
        gpsRepository.clearGpsTrajectoryPoints();
        measuredArea.setValue(0.0);
    }

    /**
     * 设置用户身高
     */
    public void setUserHeight(float height) {
        if (height > 0.5f && height < 2.5f) {
            userHeight = height;
            sensorRepository.setUserHeight(height);
        }
    }

    /**
     * 设置是否使用GPS
     */
    public void setUseGps(boolean use) {
        useGps = use;
        locatingMode.setValue(use ? "Hybrid" : "PDR");
    }

    /**
     * 校准步长
     */
    public void calibrateStepLength(float actualDistance, int stepCount) {
        sensorRepository.calibrateStepLength(actualDistance, stepCount);
    }

    /**
     * 获取当前步长的LiveData
     */
    public LiveData<Float> getStepLength() {
        return sensorRepository.getStepLength();
    }

    public LiveData<Integer> getStepCount() {
        return sensorRepository.getStepCount();
    }

    public LiveData<Float> getOrientation() {
        return sensorRepository.getOrientation();
    }

    public LiveData<float[]> getAccelerometerData() {
        return sensorRepository.getAccelerometerData();
    }

    public LiveData<float[]> getGyroscopeData() {
        return sensorRepository.getGyroscopeData();
    }

    public LiveData<float[]> getMagneticFieldData() {
        return sensorRepository.getMagneticFieldData();
    }

    public LiveData<List<TrajectoryPoint>> getTrajectoryPoints() {
        return locationRepository.getTrajectoryPoints();
    }

    public LiveData<List<TrajectoryPoint>> getRawTrajectoryPoints() {
        return locationRepository.getRawTrajectoryPoints();
    }

    public LiveData<Double> getMeasuredArea() {
        return measuredArea;
    }

    public LiveData<Boolean> getIsMeasuring() {
        return isMeasuring;
    }

    public LiveData<List<Measurement>> getSavedMeasurements() {
        return measurementRepository.getMeasurements();
    }

    public LiveData<Double> getLocationAccuracy() {
        return locationRepository.getLocationAccuracy();
    }

    public LiveData<String> getLocatingMode() {
        return locatingMode;
    }

    public LiveData<Boolean> isGpsAvailable() {
        return gpsRepository.isGpsAvailable();
    }

    public LiveData<Integer> getSatelliteCount() {
        return gpsRepository.getSatelliteCount();
    }

    public LiveData<Float> getGpsAccuracy() {
        return gpsRepository.getGpsAccuracy();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 移除永久观察者
        locationRepository.getLocatingMode().removeObserver(mode -> {});
    }
}