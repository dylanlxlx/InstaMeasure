// 位置：app/src/main/java/com/dylanlxlx/instameasure/model/GpsData.java
package com.dylanlxlx.instameasure.model;

/**
 * GPS数据模型类
 * 封装GPS原始数据
 */
public class GpsData {
    private final double latitude;    // 纬度
    private final double longitude;   // 经度
    private final double altitude;    // 海拔
    private final float accuracy;     // 精度（米）
    private final float speed;        // 速度（米/秒）
    private final float bearing;      // 方位角（度）
    private final long timestamp;     // 时间戳
    private final int satelliteCount; // 卫星数量

    public GpsData(double latitude, double longitude, double altitude,
                   float accuracy, float speed, float bearing,
                   long timestamp, int satelliteCount) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.timestamp = timestamp;
        this.satelliteCount = satelliteCount;
    }

    // Getters
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
    public float getAccuracy() { return accuracy; }
    public float getSpeed() { return speed; }
    public float getBearing() { return bearing; }
    public long getTimestamp() { return timestamp; }
    public int getSatelliteCount() { return satelliteCount; }
}