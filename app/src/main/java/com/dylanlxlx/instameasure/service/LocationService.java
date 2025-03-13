package com.dylanlxlx.instameasure.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dylanlxlx.instameasure.data.repository.LocationRepository;
import com.dylanlxlx.instameasure.data.repository.SensorRepository;

/**
 * Service for GPS location tracking.
 * Can be used for more accurate outdoor measurements.
 */
public class LocationService extends Service implements LocationListener {
    private static final String TAG = LocationService.class.getSimpleName();
    private static final long MIN_TIME_MS = 1000; // 1 second
    private static final float MIN_DISTANCE_M = 0.5f; // 0.5 meters

    private LocationManager locationManager;
    private LocationRepository locationRepository;
    private SensorRepository sensorRepository;

    private Location lastLocation;
    private boolean isTracking = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");

        // Get repositories
        locationRepository = LocationRepository.getInstance();
        sensorRepository = SensorRepository.getInstance();

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
        Log.d(TAG, "LocationService destroyed");
    }

    /**
     * Start GPS location tracking
     */
    public void startTracking() {
        if (locationManager != null && !isTracking) {
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        this
                );
                isTracking = true;
                Log.d(TAG, "Started location tracking");

                // Initialize the trajectory with current position
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    lastLocation = location;
                    locationRepository.clearTrajectoryPoints();
                    locationRepository.addTrajectoryPoint(0, 0); // Starting point
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission denied", e);
            }
        }
    }

    /**
     * Stop GPS location tracking
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
     * Process location updates using sensor data
     * @param stepLength The length of a step in meters
     * @param orientation The orientation in degrees (0=North, 90=East)
     */
    public void processStepWithOrientation(float stepLength, float orientation) {
        if (isTracking) {
            // In GPS mode, we can use this to supplement with PDR
            locationRepository.addRelativePosition(stepLength, orientation);
        }
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (lastLocation == null) {
            lastLocation = location;
            return;
        }

        // Calculate displacement from last location
        float[] results = new float[3];
        Location.distanceBetween(
                lastLocation.getLatitude(), lastLocation.getLongitude(),
                location.getLatitude(), location.getLongitude(),
                results
        );

        float distance = results[0]; // Distance in meters
        float bearing = results[1]; // Bearing in degrees

        // Add to trajectory
        float dx = (float)(distance * Math.sin(Math.toRadians(bearing)));
        float dy = (float)(distance * Math.cos(Math.toRadians(bearing)));

        // Add point
        locationRepository.addTrajectoryPoint(dx, dy);

        // Update last location
        lastLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Not used in newer Android versions
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "Provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d(TAG, "Provider disabled: " + provider);
    }
}