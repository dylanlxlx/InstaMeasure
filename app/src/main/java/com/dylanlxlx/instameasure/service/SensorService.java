package com.dylanlxlx.instameasure.service;

// 传感器持续监听

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.utils.DirectionCalculator;
import com.dylanlxlx.instameasure.utils.MathUtils;
import com.dylanlxlx.instameasure.utils.SensorFilter;

import java.util.ArrayList;
import java.util.List;

public class SensorService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer, stepDetector;
    private float[] lastAccel = new float[3];
    private int totalSteps = 0;
    private float stepLength = 0.6f; // 默认步长0.6米（需校准）

    private DirectionCalculator directionCalculator = new DirectionCalculator();
    private float[] currentAccel = new float[3];
    private float[] currentMag = new float[3];

    private List<TrajectoryPoint> trajectory = new ArrayList<>();
    private double currentX = 0.0;
    private double currentY = 0.0;

    private MutableLiveData<Double> currentDistance = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> currentArea = new MutableLiveData<>(0.0);
    private MutableLiveData<List<TrajectoryPoint>> trajectoryLiveData = new MutableLiveData<>();

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        /*
          初始化传感器
          TYPE_ACCELEROMETER: 测量在所有三个物理轴向（x、y 和 z）上施加在设备上的加速力（包括重力），以 m/s2 为单位 —— 移动侦测（摇晃、倾斜等）
          TYPE_GYROSCOPE: 测量设备在三个物理轴向（x、y 和 z）上的旋转速率，以 rad/s 为单位 —— 旋转侦测（转向、转动等）
          TYPE_MAGNETIC_FIELD: 测量所有三个物理轴向（x、y、z）上的环境地磁场，以微特斯拉（uT）为单位 —— 方向侦测（指南针、方向等）
         */
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // 注册监听（50Hz采样率）
        //SENSOR_DELAY_NORMAL: 200ms
        //SENSOR_DELAY_GAME: 20ms
        //SENSOR_DELAY_UI: 60ms
        //SENSOR_DELAY_FASTEST: 0ms
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, currentAccel, 0, 3);
                handleAccelerometer(event.values);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, currentMag, 0, 3);
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                handleStepDetection();
                break;
        }

        // 当加速度和磁力数据就绪时计算方向
        if (currentAccel != null && currentMag != null) {
            float azimuth = directionCalculator.calculateAzimuth(currentAccel, currentMag);
            updateTrajectory(azimuth);
        }
    }

    private void handleAccelerometer(float[] rawAccel) {
        // 低通滤波（alpha=0.8）
        lastAccel = SensorFilter.lowPass(rawAccel, lastAccel, 0.8f);

        // 检测步伐（示例：阈值1.5g）
        float magnitude = SensorFilter.calculateMagnitude(lastAccel);
        if (magnitude > 1.5f * 9.81f) { // 1.5倍重力加速度
            totalSteps++;
            updateDistance(totalSteps * stepLength);
        }
    }

    private void handleStepDetection() {
        totalSteps++;
        updateDistance(totalSteps * stepLength);
    }

    private void updateTrajectory(float azimuth) {
        // 将极坐标转换为直角坐标
        double radians = Math.toRadians(azimuth);
        currentX += stepLength * Math.sin(radians);
        currentY += stepLength * Math.cos(radians);

        trajectory.add(new TrajectoryPoint(currentX, currentY));
        trajectoryLiveData.postValue(new ArrayList<>(trajectory)); // 传递副本

        // 当轨迹闭合时计算面积
        if (isTrajectoryClosed()) {
            calculateAreaAndReset();
        }
    }

    private boolean isTrajectoryClosed() {
        if (trajectory.size() < 4) return false;
        TrajectoryPoint first = trajectory.get(0);
        TrajectoryPoint last = trajectory.get(trajectory.size() - 1);
        double distance = Math.hypot(first.getX() - last.getX(), first.getY() - last.getY());
        return distance < 1.0; // 闭合阈值1米
    }

    // 在轨迹闭合时触发面积计算
    private void calculateAreaAndReset() {
        double area = MathUtils.calculatePolygonArea(trajectory);
        currentArea.postValue(area);
        trajectory.clear();
        currentX = 0.0;
        currentY = 0.0;
    }

    private void updateDistance(double distance) {
        currentDistance.postValue(distance);
    }

    public LiveData<List<TrajectoryPoint>> getTrajectoryLiveData() {
        return trajectoryLiveData;
    }

    // 将LiveData给外部
    public LiveData<Double> getCurrentDistance() {
        return currentDistance;
    }

    public LiveData<Double> getCurrentArea() {
        return currentArea;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    } // 暂留空

    // 添加Binder类
    public class LocalBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }
}
