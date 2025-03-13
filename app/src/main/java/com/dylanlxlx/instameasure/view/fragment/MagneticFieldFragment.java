package com.dylanlxlx.instameasure.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fragment for displaying magnetometer data charts.
 */
public class MagneticFieldFragment extends BaseSensorChartFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set chart titles
        xAxisChart.getDescription().setText("Magnetometer X-Axis (µT)");
        yAxisChart.getDescription().setText("Magnetometer Y-Axis (µT)");
        zAxisChart.getDescription().setText("Magnetometer Z-Axis (µT)");
    }

    @Override
    protected void setupChartObservers() {
        // Observe X-axis data
        viewModel.getMagnetometerXData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(xAxisChart, entries, "X-Axis", Color.RED);
        });

        // Observe Y-axis data
        viewModel.getMagnetometerYData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(yAxisChart, entries, "Y-Axis", Color.GREEN);
        });

        // Observe Z-axis data
        viewModel.getMagnetometerZData().observe(getViewLifecycleOwner(), entries -> {
            updateChart(zAxisChart, entries, "Z-Axis", Color.BLUE);
        });
    }
}