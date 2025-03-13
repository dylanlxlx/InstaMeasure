package com.dylanlxlx.instameasure.domain;

import com.dylanlxlx.instameasure.model.SensorData;
import com.dylanlxlx.instameasure.utils.SensorFilter;
import com.dylanlxlx.instameasure.utils.StepDetector;
import com.dylanlxlx.instameasure.utils.ImprovedOrientationCalculator;
import com.dylanlxlx.instameasure.utils.DynamicStepLengthEstimator;

/**
 * SensorDataManager 用于处理传感器数据，完成滤波、步数检测和方向计算。
 */
public class SensorDataManager {

    // 传感器处理组件
    private SensorFilter sensorFilter;
    private StepDetector stepDetector;
    private ImprovedOrientationCalculator orientationCalculator;
    private DynamicStepLengthEstimator stepLengthEstimator;

    // 时间戳管理
    private long lastTimestamp = 0;

    // 当前的步长(米)
    private float currentStepLength = 0.7f;

    // 传感器数据回调接口
    public interface SensorDataCallback {
        void onStepDetected(int stepCount);
        void onOrientationCalculated(float orientation);
        void onStepLengthCalculated(float stepLength);
    }

    private SensorDataCallback callback;

    /**
     * 构造函数，初始化算法对象和滤波器
     * @param callback 数据处理回调接口
     */
    public SensorDataManager(SensorDataCallback callback) {
        this.callback = callback;

        // 初始化传感器滤波器
        // 通常 q 比 r 小
        // 滤波效果过于平滑（响应迟缓）：减小 q 或增大 r
        // 滤波后噪声仍然明显：增大 q 或减小 r
        sensorFilter = new SensorFilter(0.01, 0.1);

        // 初始化步数检测器
        stepDetector = new StepDetector(new StepDetector.StepCallback() {
            @Override
            public void onStepDetected(int stepCount) {
                handleStepDetected(stepCount);
            }

            @Override
            public void onWalkingStateChanged(String newState) {
                // 可以在这里处理行走状态变化
            }
        });

        // 初始化方向计算器
        orientationCalculator = new ImprovedOrientationCalculator();

        // 初始化动态步长估计器
        stepLengthEstimator = new DynamicStepLengthEstimator();
    }

    /**
     * 设置用户身高
     * @param height 身高(米)
     */
    public void setUserHeight(float height) {
        if (height > 0.5f && height < 2.5f) {
            stepLengthEstimator.setUserHeight(height);
        }
    }

    /**
     * 校准步长
     * @param actualDistance 实际步行距离(米)
     * @param stepCount 步数
     */
    public void calibrateStepLength(float actualDistance, int stepCount) {
        stepLengthEstimator.calibrate(actualDistance, stepCount);
    }

    /**
     * 处理传感器数据
     * @param sensorData 封装后的传感器数据
     */
    public void processSensorData(SensorData sensorData) {
        // 当前时间戳
        long currentTimestamp = System.currentTimeMillis();

        // 首次调用初始化时间戳
        if (lastTimestamp == 0) {
            lastTimestamp = currentTimestamp;
        }

        // 对传感器数据进行滤波
        float[] filteredAccel = sensorFilter.filterAccelerometer(sensorData.getAccelerometer());
        float[] filteredMag = sensorFilter.filterMagneticField(sensorData.getMagnetometer());
        float[] filteredGyro = sensorFilter.filterGyroscope(sensorData.getGyroscope());

        // 处理步数检测（此处只用加速度数据即可）
        stepDetector.processSensorData(filteredAccel);

        // 计算方向，传入滤波后的加速度与地磁数据和陀螺仪数据
        float orientation = orientationCalculator.calculateAzimuth(
                filteredAccel, filteredMag, filteredGyro, currentTimestamp
        );

        // 通过回调返回处理结果
        if (callback != null) {
            callback.onOrientationCalculated(orientation);
        }

        // 更新时间戳
        lastTimestamp = currentTimestamp;
    }

    /**
     * 处理步数检测响应
     * @param stepCount 累计步数
     */
    private void handleStepDetected(int stepCount) {
        if (callback == null) return;

        // 计算加速度幅值
        float[] accel = stepDetector.getLastAcceleration();
        float accelMagnitude = 0;
        if (accel != null) {
            accelMagnitude = (float) Math.sqrt(
                    accel[0] * accel[0] +
                            accel[1] * accel[1] +
                            accel[2] * accel[2]
            );
        }

        // 动态估计步长
        currentStepLength = stepLengthEstimator.estimateStepLength(accelMagnitude);

        // 通知回调
        callback.onStepDetected(stepCount);
        callback.onStepLengthCalculated(currentStepLength);
    }

    /**
     * 使用GPS方位校准方向计算
     * @param gpsBearing GPS方位角(度)
     */
    public void calibrateWithGps(float gpsBearing) {
        orientationCalculator.calibrateWithGps(gpsBearing);
    }

    /**
     * 重置传感器数据处理器
     */
    public void reset() {
        stepDetector.reset();
        orientationCalculator.reset();
        lastTimestamp = 0;
    }

    /**
     * 获取当前步长
     * @return 当前步长(米)
     */
    public float getCurrentStepLength() {
        return currentStepLength;
    }
}