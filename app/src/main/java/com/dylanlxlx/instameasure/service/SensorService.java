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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.model.SensorData;
import com.dylanlxlx.instameasure.domain.SensorDataManager;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = SensorService.class.getSimpleName();

    // LiveData 用于暴露步数和方向数据
    private MutableLiveData<Integer> stepCountLiveData = new MutableLiveData<>();
    private MutableLiveData<Float> orientationLiveData = new MutableLiveData<>();

    // 用于展示原始传感器数据
    private MutableLiveData<float[]> accelerometerLiveData = new MutableLiveData<>();
    private MutableLiveData<float[]> gyroscopeLiveData = new MutableLiveData<>();
    private MutableLiveData<float[]> magneticFieldLiveData = new MutableLiveData<>();

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;
    private Sensor gyroscopeSensor;

    // 新增 SensorDataManager 对象，用于数据处理
    private SensorDataManager sensorDataManager;

    // 用于存储各传感器原始数据
    private float[] accelerometerValues = null;
    private float[] magneticValues = null;
    private float[] gyroscopeValues = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务已创建");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // 获取传感器
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // 初始化 SensorDataManager，并传入回调，将处理结果通过 LiveData 暴露给外部
        sensorDataManager = new SensorDataManager(new SensorDataManager.SensorDataCallback() {
            @Override
            public void onStepDetected(int stepCount) {
                stepCountLiveData.postValue(stepCount);
            }

            @Override
            public void onOrientationCalculated(float orientation) {
                orientationLiveData.postValue(orientation);
            }
        });

        // 注册传感器监听
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "加速度传感器已注册");
        } else {
            Log.e(TAG, "加速度传感器不可用");
        }

        if (magneticFieldSensor != null) {
            sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "地磁传感器已注册");
        } else {
            Log.e(TAG, "地磁传感器不可用");
        }

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "陀螺仪传感器已注册");
        } else {
            Log.e(TAG, "陀螺仪传感器不可用");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务已启动");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务已销毁");
        sensorManager.unregisterListener(this);
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

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = values;
            accelerometerLiveData.postValue(accelerometerValues);
        } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = values;
            magneticFieldLiveData.postValue(magneticValues);
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            gyroscopeValues = values;
            gyroscopeLiveData.postValue(gyroscopeValues);
        }

        // 当获取到加速度、地磁和陀螺仪数据后，封装成 SensorData 传递给 SensorDataManager
        if (accelerometerValues != null && magneticValues != null && gyroscopeValues != null) {
            SensorData sensorData = new SensorData(accelerometerValues, gyroscopeValues, magneticValues);
            sensorDataManager.processSensorData(sensorData);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 不需要处理
    }

    // 对外提供 LiveData
    public LiveData<Integer> getStepCountLiveData() {
        return stepCountLiveData;
    }

    public LiveData<Float> getOrientationLiveData() {
        return orientationLiveData;
    }

    public LiveData<float[]> getAccelerometerLiveData() {
        return accelerometerLiveData;
    }

    public LiveData<float[]> getGyroscopeLiveData() {
        return gyroscopeLiveData;
    }

    public LiveData<float[]> getMagneticFieldLiveData() {
        return magneticFieldLiveData;
    }
}
