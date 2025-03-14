package com.dylanlxlx.instameasure.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.domain.SensorDataManager;
import com.dylanlxlx.instameasure.model.SensorData;

/**
 * 传感器相关数据的存储库
 * 应用程序中所有传感器数据的单一事实来源
 */
public class SensorRepository implements SensorDataManager.SensorDataCallback {
    private static volatile SensorRepository instance;

    // 要观察的 LiveData 对象
    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>(0);
    private final MutableLiveData<Float> orientation = new MutableLiveData<>(0f);
    private final MutableLiveData<float[]> accelerometerData = new MutableLiveData<>(new float[3]);
    private final MutableLiveData<float[]> gyroscopeData = new MutableLiveData<>(new float[3]);
    private final MutableLiveData<float[]> magneticFieldData = new MutableLiveData<>(new float[3]);
    private final MutableLiveData<Float> stepLength = new MutableLiveData<>(0.7f);

    // SensorDataManager 用于处理原始传感器数据
    private final SensorDataManager sensorDataManager;

    // 私有构造函数，用于防止直接实例化
    private SensorRepository() {
        // 使用此存储库作为回调初始化 SensorDataManager
        sensorDataManager = new SensorDataManager(this);
    }

    public static SensorRepository getInstance() {
        if (instance == null) {
            synchronized (SensorRepository.class) {
                if (instance == null) {
                    instance = new SensorRepository();
                }
            }
        }
        return instance;
    }

    /**
     * 处理新的传感器数据
     * @param sensorData 原始传感器数据
     */
    public void processSensorData(SensorData sensorData) {
        // 将数据传递给 SensorDataManager 进行处理
        sensorDataManager.processSensorData(sensorData);

        // 使用原始传感器值更新 LiveData
        accelerometerData.postValue(sensorData.getAccelerometer());
        gyroscopeData.postValue(sensorData.getGyroscope());
        magneticFieldData.postValue(sensorData.getMagnetometer());
    }

    @Override
    public void onStepDetected(int count) {
        stepCount.postValue(count);
    }

    @Override
    public void onOrientationCalculated(float azimuth) {
        orientation.postValue(azimuth);
    }

    @Override
    public void onStepLengthCalculated(float length) {
        stepLength.postValue(length);
    }

    /**
     * 重置传感器处理器
     */
    public void resetSensorProcessors() {
        sensorDataManager.reset();
        stepCount.postValue(0);
    }

    /**
     * 使用GPS方位角校准方向
     * @param gpsBearing GPS方位角(度)
     */
    public void calibrateOrientationWithGps(float gpsBearing) {
        sensorDataManager.calibrateWithGps(gpsBearing);
    }

    /**
     * 设置用户身高
     * @param height 身高(米)
     */
    public void setUserHeight(float height) {
        sensorDataManager.setUserHeight(height);
    }

    /**
     * 校准步长
     * @param actualDistance 实际距离(米)
     * @param stepCount 步数
     */
    public void calibrateStepLength(float actualDistance, int stepCount) {
        sensorDataManager.calibrateStepLength(actualDistance, stepCount);
    }

    public LiveData<Integer> getStepCount() {
        return stepCount;
    }

    public LiveData<Float> getOrientation() {
        return orientation;
    }

    public LiveData<Float> getStepLength() {
        return stepLength;
    }

    public LiveData<float[]> getAccelerometerData() {
        return accelerometerData;
    }

    public LiveData<float[]> getGyroscopeData() {
        return gyroscopeData;
    }

    public LiveData<float[]> getMagneticFieldData() {
        return magneticFieldData;
    }
}