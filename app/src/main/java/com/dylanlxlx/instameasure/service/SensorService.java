package com.dylanlxlx.instameasure.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dylanlxlx.instameasure.model.SensorData;
import com.dylanlxlx.instameasure.data.repository.SensorRepository;

/**
 * 用于在后台收集传感器数据的服务。
 * 处理数据并将其传递到 SensorRepository。
 */
public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = SensorService.class.getSimpleName();

    // 系统服务
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;
    private Sensor gyroscopeSensor;

    // Repository
    private SensorRepository sensorRepository;

    // 原始传感器数据存储
    private float[] accelerometerValues = null;
    private float[] magneticValues = null;
    private float[] gyroscopeValues = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SensorService created");

        // 获取repository实例
        sensorRepository = SensorRepository.getInstance();

        // Initialize sensors
        initializeSensors();
    }

    /**
     * 初始化和注册传感器
     */
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // 获取传感器对象
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // 为每个传感器注册侦听器
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "已注册加速度计");
        } else {
            Log.e(TAG, "加速度计不可用");
        }

        if (magneticFieldSensor != null) {
            sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "已注册磁场传感器");
        } else {
            Log.e(TAG, "磁场传感器不可用");
        }

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "陀螺仪已注册");
        } else {
            Log.e(TAG, "陀螺仪不可用");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SensorService 已启动");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SensorService 已销毁");

        // 取消注册传感器侦听器
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float[] values = event.values.clone();

        // 根据传感器类型存储值
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = values;
        } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = values;
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            gyroscopeValues = values;
        }

        // 当拥有来自所有传感器的值时处理数据
        if (accelerometerValues != null && magneticValues != null && gyroscopeValues != null) {
            SensorData sensorData = new SensorData(
                    accelerometerValues,
                    gyroscopeValues,
                    magneticValues
            );

            // 传递到repository进行处理
            sensorRepository.processSensorData(sensorData);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}