package com.dylanlxlx.instameasure.domain;

import com.dylanlxlx.instameasure.model.SensorData;
import com.dylanlxlx.instameasure.utils.SensorFilter;
import com.dylanlxlx.instameasure.utils.StepDetector;
import com.dylanlxlx.instameasure.utils.OrientationCalculator;

/**
 * SensorDataManager 用于处理传感器数据，完成滤波、步数检测和方向计算。
 */
public class SensorDataManager {

    private SensorFilter sensorFilter;
    private StepDetector stepDetector;
    private OrientationCalculator orientationCalculator;
    private SensorDataCallback callback;

    /**
     * 数据处理回调接口
     */
    public interface SensorDataCallback {
        void onStepDetected(int stepCount);

        void onOrientationCalculated(float orientation);
    }

    /**
     * 构造函数，初始化算法对象和滤波器
     *
     * @param callback 数据处理回调接口
     */
    public SensorDataManager(SensorDataCallback callback) {
        this.callback = callback;
        // 初始化传感器滤波器
        // 通常 q 比 r 小
        // 滤波效果过于平滑（响应迟缓）：减小 q 或增大 r
        // 滤波后噪声仍然明显：增大 q 或减小 r
        sensorFilter = new SensorFilter(0.01, 0.1);
        stepDetector = new StepDetector(stepCount -> {
            if (callback != null) {
                callback.onStepDetected(stepCount);
            }
        });
        orientationCalculator = new OrientationCalculator();
    }

    /**
     * 处理传感器数据
     *
     * @param sensorData 封装后的传感器数据
     */
    public void processSensorData(SensorData sensorData) {
        // 对传感器数据进行滤波
        float[] filteredAccel = sensorFilter.filterAccelerometer(sensorData.getAccelerometer());
        float[] filteredMag = sensorFilter.filterMagneticField(sensorData.getMagnetometer());
        float[] filteredGyro = sensorFilter.filterGyroscope(sensorData.getGyroscope());

        // 处理步数检测（此处只用加速度数据即可）
        stepDetector.processSensorData(filteredAccel);

        // 计算方向，传入滤波后的加速度与地磁数据
        float orientation = orientationCalculator.calculateAzimuth(filteredAccel, filteredMag);

        // 通过回调返回处理结果
        if (callback != null) {
            callback.onOrientationCalculated(orientation);
        }
    }
}
