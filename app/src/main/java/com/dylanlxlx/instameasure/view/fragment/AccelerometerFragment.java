package com.dylanlxlx.instameasure.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fragment for displaying accelerometer data charts.
 */
public class AccelerometerFragment extends BaseSensorChartFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set chart titles
        xAxisChart.getDescription().setText("Accelerometer X-Axis (m/s²)");
        yAxisChart.getDescription().setText("Accelerometer Y-Axis (m/s²)");
        zAxisChart.getDescription().setText("Accelerometer Z-Axis (m/s²)");
    }

    @Override
    protected void setupChartObservers() {
        // Observe X-axis data
        viewModel.getAccelerometerXData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(xAxisChart, entries, "X-Axis", Color.RED);
        });

        // Observe Y-axis data
        viewModel.getAccelerometerYData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(yAxisChart, entries, "Y-Axis", Color.GREEN);
        });

        // Observe Z-axis data
        viewModel.getAccelerometerZData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(zAxisChart, entries, "Z-Axis", Color.BLUE);
        });
    }
}