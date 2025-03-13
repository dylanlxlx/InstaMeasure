// 位置：app/src/main/java/com/dylanlxlx/instameasure/utils/LocationUtils.java
package com.dylanlxlx.instameasure.utils;

import android.location.Location;

/**
 * 位置计算工具类
 * 提供坐标转换和距离计算等功能
 */
public class LocationUtils {

    // 地球半径（米）
    private static final double EARTH_RADIUS = 6371000;

    /**
     * 将地理坐标（经纬度）转换为本地坐标（米）
     * 使用平面投影近似，适用于小范围区域
     *
     * @param latitude 纬度
     * @param longitude 经度
     * @param originLatitude 原点纬度
     * @param originLongitude 原点经度
     * @return 本地坐标 [x, y]，其中x为东向距离，y为北向距离
     */
    public static double[] geoToLocalCoordinates(double latitude, double longitude,
                                                 double originLatitude, double originLongitude) {
        // 将经纬度转换为弧度
        double lat = Math.toRadians(latitude);
        double lon = Math.toRadians(longitude);
        double lat0 = Math.toRadians(originLatitude);
        double lon0 = Math.toRadians(originLongitude);

        // 计算经度差对应的弧度
        double deltaLon = lon - lon0;

        // 计算东向距离（x坐标）
        double x = EARTH_RADIUS * Math.cos(lat0) * deltaLon;

        // 计算北向距离（y坐标）
        double y = EARTH_RADIUS * (lat - lat0);

        return new double[]{x, y};
    }

    /**
     * 将本地坐标（米）转换为地理坐标（经纬度）
     *
     * @param x 东向距离（米）
     * @param y 北向距离（米）
     * @param originLatitude 原点纬度
     * @param originLongitude 原点经度
     * @return 地理坐标 [latitude, longitude]
     */
    public static double[] localToGeoCoordinates(double x, double y,
                                                 double originLatitude, double originLongitude) {
        // 将原点经纬度转换为弧度
        double lat0 = Math.toRadians(originLatitude);
        double lon0 = Math.toRadians(originLongitude);

        // 计算纬度变化
        double deltaLat = y / EARTH_RADIUS;

        // 计算经度变化
        double deltaLon = x / (EARTH_RADIUS * Math.cos(lat0));

        // 计算新的纬度和经度（弧度）
        double lat = lat0 + deltaLat;
        double lon = lon0 + deltaLon;

        // 转换回角度
        return new double[]{Math.toDegrees(lat), Math.toDegrees(lon)};
    }

    /**
     * 计算两个地理坐标点之间的距离（米）
     * 使用Haversine公式
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 两点之间的距离（米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 将经纬度转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine公式
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return EARTH_RADIUS * c;
    }

    /**
     * 计算两点之间的方位角（度）
     * 0度为正北，顺时针增加
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 方位角（度）
     */
    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        // 将经纬度转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // 计算方位角
        double dLon = lon2Rad - lon1Rad;
        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
        double bearing = Math.atan2(y, x);

        // 转换为角度，并调整为0-360度范围
        double bearingDeg = Math.toDegrees(bearing);
        return (bearingDeg + 360) % 360;
    }

    /**
     * 使用卡尔曼滤波平滑GPS轨迹
     *
     * @param location 当前位置
     * @param filters 卡尔曼滤波器
     * @return 过滤后的位置
     */
    public static Location applyKalmanFilter(Location location, KalmanFilter1D[] filters) {
        if (location == null || filters == null || filters.length < 2) {
            return location;
        }

        // 过滤经度和纬度
        double filteredLat = filters[0].filter(location.getLatitude());
        double filteredLon = filters[1].filter(location.getLongitude());

        // 创建新的位置对象
        Location filteredLocation = new Location(location);
        filteredLocation.setLatitude(filteredLat);
        filteredLocation.setLongitude(filteredLon);

        return filteredLocation;
    }
}