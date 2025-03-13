package com.dylanlxlx.instameasure.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dylanlxlx.instameasure.data.repository.SensorRepository;
import com.dylanlxlx.instameasure.data.repository.LocationRepository;
import com.dylanlxlx.instameasure.data.repository.MeasurementRepository;
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.model.Measurement;
import com.dylanlxlx.instameasure.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 测量功能的 ViewModel
 * 处理存储库和 UI 之间的通信
 */
public class MeasureViewModel extends ViewModel {
    private final SensorRepository sensorRepository;
    private final LocationRepository locationRepository;
    private final MeasurementRepository measurementRepository;

    private final MutableLiveData<Double> measuredArea = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isMeasuring = new MutableLiveData<>(false);

    public MeasureViewModel() {
        // 获取存储库实例
        this.sensorRepository = SensorRepository.getInstance();
        this.locationRepository = LocationRepository.getInstance();
        this.measurementRepository = MeasurementRepository.getInstance();
    }

    /**
     * 开始新的测量会话
     */
    public void startMeasurement() {
        locationRepository.clearTrajectoryPoints();
        locationRepository.addTrajectoryPoint(0.0, 0.0); // Add starting point
        isMeasuring.setValue(true);
    }

    /**
     * 停止当前测量会话
     */
    public void stopMeasurement() {
        isMeasuring.setValue(false);
        calculateArea();
    }

    /**
     * 使用鞋带定理计算当前轨迹的面积
     */
    public void calculateArea() {
        List<TrajectoryPoint> points = locationRepository.getTrajectoryPoints().getValue();
        if (points != null && points.size() > 2) {
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
        measuredArea.setValue(0.0);
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

    public LiveData<Double> getMeasuredArea() {
        return measuredArea;
    }

    public LiveData<Boolean> getIsMeasuring() {
        return isMeasuring;
    }

    public LiveData<List<Measurement>> getSavedMeasurements() {
        return measurementRepository.getMeasurements();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}