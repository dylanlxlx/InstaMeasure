package com.dylanlxlx.instameasure.service;

import android.app.Service;
import android.content.Intent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.dylanlxlx.instameasure.data.repository.GpsRepository;
import com.dylanlxlx.instameasure.data.repository.LocationRepository;
import com.dylanlxlx.instameasure.data.repository.SensorRepository;
import com.dylanlxlx.instameasure.domain.GpsDataManager;
import com.dylanlxlx.instameasure.model.GpsData;

/**
 * 位置服务
 * 负责GPS定位和轨迹跟踪
 */
public class LocationService extends Service implements LocationListener, GpsDataManager.GpsDataCallback {
    private static final String TAG = LocationService.class.getSimpleName();
    private static final long MIN_TIME_MS = 1000; // 1秒
    private static final float MIN_DISTANCE_M = 0.5f; // 0.5米

    private LocationManager locationManager;
    private GpsRepository gpsRepository;
    private LocationRepository locationRepository;
    private SensorRepository sensorRepository;
    private GpsDataManager gpsDataManager;

    private Location lastLocation;
    private boolean isTracking = false;
    private int satelliteCount = 0;

    // GNSS状态监听器（适用于API 24及以上）
    @RequiresApi(api = Build.VERSION_CODES.N)
    private final GnssStatus.Callback gnssStatusCallback = new GnssStatus.Callback() {
        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            satelliteCount = status.getSatelliteCount();
        }

        @Override
        public void onStarted() {
            Log.d(TAG, "GNSS started");
        }

        @Override
        public void onStopped() {
            Log.d(TAG, "GNSS stopped");
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            Log.d(TAG, "GNSS first fix in " + ttffMillis + " ms");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");

        // 获取存储库
        gpsRepository = GpsRepository.getInstance();
        locationRepository = LocationRepository.getInstance();
        sensorRepository = SensorRepository.getInstance();

        // 初始化位置管理器
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 初始化GPS数据管理器
        gpsDataManager = new GpsDataManager(this);

        // 注册GNSS状态监听器（适用于API 24及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                locationManager.registerGnssStatusCallback(gnssStatusCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "GNSS status permission denied", e);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();

        // 注销GNSS状态监听器（适用于API 24及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "Error unregistering GNSS status callback", e);
            }
        }

        Log.d(TAG, "LocationService destroyed");
    }

    /**
     * 开始GPS定位跟踪
     */
    public void startTracking() {
        if (locationManager != null && !isTracking) {
            try {
                // 请求位置更新
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        this
                );
                isTracking = true;
                Log.d(TAG, "Started location tracking");

                // 获取当前位置初始化轨迹
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    lastLocation = location;
                    // 处理初始GPS位置
                    gpsDataManager.processGpsLocation(location);
                    // 清除轨迹点
                    locationRepository.clearTrajectoryPoints();
                    gpsRepository.clearGpsTrajectoryPoints();
                    locationRepository.addTrajectoryPoint(0, 0); // 起点
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission denied", e);
            }
        }
    }

    /**
     * 停止GPS定位跟踪
     */
    public void stopTracking() {
        if (locationManager != null && isTracking) {
            try {
                locationManager.removeUpdates(this);
                isTracking = false;
                Log.d(TAG, "Stopped location tracking");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when stopping location tracking", e);
            }
        }
    }

    /**
     * 使用传感器数据处理步进和方向
     * @param stepLength 步长（米）
     * @param orientation 方向（度，0=北，90=东）
     */
    public void processStepWithOrientation(float stepLength, float orientation) {
        if (isTracking) {
            // 在GPS模式下，可以使用PDR补充定位
            locationRepository.addRelativePosition(stepLength, orientation);
        }
    }

    // LocationListener接口方法
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // 将GPS位置传递给数据管理器处理
        gpsDataManager.processGpsLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // 不在较新的Android版本中使用
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "Provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d(TAG, "Provider disabled: " + provider);
        gpsRepository.setGpsUnavailable();
    }

    // GpsDataCallback接口方法
    @Override
    public void onGpsDataProcessed(GpsData gpsData) {
        // 更新GPS数据到存储库
        gpsData = updateSatelliteCount(gpsData);
        gpsRepository.updateGpsData(gpsData);

        // 创建Location对象用于计算位移
        Location newLocation = new Location("gps");
        newLocation.setLatitude(gpsData.getLatitude());
        newLocation.setLongitude(gpsData.getLongitude());

        if (lastLocation != null) {
            // 计算从上一个位置的位移
            float[] results = new float[3];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    newLocation.getLatitude(), newLocation.getLongitude(),
                    results
            );

            float distance = results[0]; // 距离（米）
            float bearing = results[1]; // 方位角（度）

            // 计算东向和北向位移
            float dx = (float)(distance * Math.sin(Math.toRadians(bearing)));
            float dy = (float)(distance * Math.cos(Math.toRadians(bearing)));

            // 添加轨迹点
            locationRepository.addTrajectoryPoint(dx, dy);
        }

        // 更新最后位置
        lastLocation = newLocation;
    }

    @Override
    public void onGpsUnavailable() {
        gpsRepository.setGpsUnavailable();
    }

    /**
     * 更新卫星计数
     */
    private GpsData updateSatelliteCount(GpsData gpsData) {
        if (satelliteCount > 0) {
            return new GpsData(
                    gpsData.getLatitude(),
                    gpsData.getLongitude(),
                    gpsData.getAltitude(),
                    gpsData.getAccuracy(),
                    gpsData.getSpeed(),
                    gpsData.getBearing(),
                    gpsData.getTimestamp(),
                    satelliteCount
            );
        }
        return gpsData;
    }

    /**
     * 获取当前GPS状态
     * @return GPS是否可用
     */
    public boolean isGpsEnabled() {
        return locationManager != null &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * 获取当前卫星数量
     * @return 卫星数量
     */
    public int getSatelliteCount() {
        return satelliteCount;
    }
}