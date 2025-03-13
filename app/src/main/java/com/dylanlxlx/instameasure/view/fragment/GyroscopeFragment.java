package com.dylanlxlx.instameasure.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fragment for displaying gyroscope data charts.
 */
public class GyroscopeFragment extends BaseSensorChartFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set chart titles
        xAxisChart.getDescription().setText("Gyroscope X-Axis (rad/s)");
        yAxisChart.getDescription().setText("Gyroscope Y-Axis (rad/s)");
        zAxisChart.getDescription().setText("Gyroscope Z-Axis (rad/s)");
    }

    @Override
    protected void setupChartObservers() {
        // Observe X-axis data
        viewModel.getGyroscopeXData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(xAxisChart, entries, "X-Axis", Color.RED);
        });

        // Observe Y-axis data
        viewModel.getGyroscopeYData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(yAxisChart, entries, "Y-Axis", Color.GREEN);
        });

        // Observe Z-axis data
        viewModel.getGyroscopeZData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(zAxisChart, entries, "Z-Axis", Color.BLUE);
        });
    }
}