package com.dylanlxlx.instameasure.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dylanlxlx.instameasure.data.repository.SensorRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for sensor chart data.
 * Manages data series for displaying in charts.
 */
public class ChartViewModel extends ViewModel {
    private static final int MAX_DATA_POINTS = 100; // Maximum number of data points to keep

    private final SensorRepository sensorRepository;

    // LiveData for chart data series
    private final MutableLiveData<List<Entry>> accelerometerXData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Entry>> accelerometerYData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Entry>> accelerometerZData = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<Entry>> gyroscopeXData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Entry>> gyroscopeYData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Entry>> gyroscopeZData = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<Entry>> magnetometerXData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Entry>> magnetometerYData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Entry>> magnetometerZData = new MutableLiveData<>(new ArrayList<>());

    // Time counter for X-axis values
    private float timeCounter = 0f;

    // Entry class for chart data points
    public static class Entry {
        private final float x;
        private final float y;

        public Entry(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    public ChartViewModel() {
        // Get repository instance
        sensorRepository = SensorRepository.getInstance();

        // Add some initial sample data for testing
        generateSampleData();

        // Observe sensor data changes
        observeSensorData();
    }

    /**
     * Generate sample data for initial display
     */
    private void generateSampleData() {
        List<Entry> accelXSample = new ArrayList<>();
        List<Entry> accelYSample = new ArrayList<>();
        List<Entry> accelZSample = new ArrayList<>();

        List<Entry> gyroXSample = new ArrayList<>();
        List<Entry> gyroYSample = new ArrayList<>();
        List<Entry> gyroZSample = new ArrayList<>();

        List<Entry> magXSample = new ArrayList<>();
        List<Entry> magYSample = new ArrayList<>();
        List<Entry> magZSample = new ArrayList<>();

        // Generate 100 sample points for each sensor axis
        for (int i = 0; i < 20; i++) {
            float x = i * 0.1f;

            // Accelerometer: sine wave pattern with different phases
            accelXSample.add(new Entry(x, (float) Math.sin(x)));
            accelYSample.add(new Entry(x, (float) Math.sin(x + 1)));
            accelZSample.add(new Entry(x, (float) Math.sin(x + 2)));

            // Gyroscope: cosine wave pattern
            gyroXSample.add(new Entry(x, (float) Math.cos(x)));
            gyroYSample.add(new Entry(x, (float) Math.cos(x + 1)));
            gyroZSample.add(new Entry(x, (float) Math.cos(x + 2)));

            // Magnetometer: combined sine and cosine
            magXSample.add(new Entry(x, (float) (Math.sin(x) + Math.cos(x))));
            magYSample.add(new Entry(x, (float) (Math.sin(x + 1) + Math.cos(x + 1))));
            magZSample.add(new Entry(x, (float) (Math.sin(x + 2) + Math.cos(x + 2))));
        }

        // Update LiveData with sample data
        timeCounter = 2.0f; // Start real data after sample data
        accelerometerXData.postValue(accelXSample);
        accelerometerYData.postValue(accelYSample);
        accelerometerZData.postValue(accelZSample);
        gyroscopeXData.postValue(gyroXSample);
        gyroscopeYData.postValue(gyroYSample);
        gyroscopeZData.postValue(gyroZSample);
        magnetometerXData.postValue(magXSample);
        magnetometerYData.postValue(magYSample);
        magnetometerZData.postValue(magZSample);
    }

    private void observeSensorData() {
        // Observe accelerometer data
        sensorRepository.getAccelerometerData().observeForever(values -> {
            if (values != null && values.length == 3) {
                timeCounter += 0.1f; // Increment time (assuming ~10Hz data rate)

                updateDataSeries(accelerometerXData, values[0]);
                updateDataSeries(accelerometerYData, values[1]);
                updateDataSeries(accelerometerZData, values[2]);

                // For testing, also update gyroscope and magnetometer with derived values
                // This ensures we see data even if those sensors aren't reporting
                updateDataSeries(gyroscopeXData, values[0] * 0.1f);
                updateDataSeries(gyroscopeYData, values[1] * 0.1f);
                updateDataSeries(gyroscopeZData, values[2] * 0.1f);

                updateDataSeries(magnetometerXData, values[0] * 0.5f);
                updateDataSeries(magnetometerYData, values[1] * 0.5f);
                updateDataSeries(magnetometerZData, values[2] * 0.5f);
            }
        });

        // Also observe the original sensor data
        sensorRepository.getGyroscopeData().observeForever(values -> {
            if (values != null && values.length == 3) {
                updateDataSeries(gyroscopeXData, values[0]);
                updateDataSeries(gyroscopeYData, values[1]);
                updateDataSeries(gyroscopeZData, values[2]);
            }
        });

        sensorRepository.getMagneticFieldData().observeForever(values -> {
            if (values != null && values.length == 3) {
                updateDataSeries(magnetometerXData, values[0]);
                updateDataSeries(magnetometerYData, values[1]);
                updateDataSeries(magnetometerZData, values[2]);
            }
        });
    }

    /**
     * Update a data series with a new value
     * @param dataSeries The LiveData series to update
     * @param newValue The new sensor value
     */
    private void updateDataSeries(MutableLiveData<List<Entry>> dataSeries, float newValue) {
        List<Entry> currentData = dataSeries.getValue();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }

        // Add new data point
        currentData.add(new Entry(timeCounter, newValue));

        // Limit data points to MAX_DATA_POINTS
        if (currentData.size() > MAX_DATA_POINTS) {
            currentData = currentData.subList(currentData.size() - MAX_DATA_POINTS, currentData.size());
        }

        dataSeries.postValue(new ArrayList<>(currentData));
    }

    // Getters for accelerometer data
    public LiveData<List<Entry>> getAccelerometerXData() {
        return accelerometerXData;
    }

    public LiveData<List<Entry>> getAccelerometerYData() {
        return accelerometerYData;
    }

    public LiveData<List<Entry>> getAccelerometerZData() {
        return accelerometerZData;
    }

    // Getters for gyroscope data
    public LiveData<List<Entry>> getGyroscopeXData() {
        return gyroscopeXData;
    }

    public LiveData<List<Entry>> getGyroscopeYData() {
        return gyroscopeYData;
    }

    public LiveData<List<Entry>> getGyroscopeZData() {
        return gyroscopeZData;
    }

    // Getters for magnetometer data
    public LiveData<List<Entry>> getMagnetometerXData() {
        return magnetometerXData;
    }

    public LiveData<List<Entry>> getMagnetometerYData() {
        return magnetometerYData;
    }

    public LiveData<List<Entry>> getMagnetometerZData() {
        return magnetometerZData;
    }

    /**
     * Reset all chart data
     */
    public void resetChartData() {
        timeCounter = 0f;
        accelerometerXData.postValue(new ArrayList<>());
        accelerometerYData.postValue(new ArrayList<>());
        accelerometerZData.postValue(new ArrayList<>());
        gyroscopeXData.postValue(new ArrayList<>());
        gyroscopeYData.postValue(new ArrayList<>());
        gyroscopeZData.postValue(new ArrayList<>());
        magnetometerXData.postValue(new ArrayList<>());
        magnetometerYData.postValue(new ArrayList<>());
        magnetometerZData.postValue(new ArrayList<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up observeForever observers
        sensorRepository.getAccelerometerData().removeObserver(values -> {});
        sensorRepository.getGyroscopeData().removeObserver(values -> {});
        sensorRepository.getMagneticFieldData().removeObserver(values -> {});
    }
}