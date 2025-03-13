package com.dylanlxlx.instameasure.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.viewmodel.ChartViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Fragment for all sensor chart fragments.
 * Provides common functionality for chart setup and data handling.
 */
public abstract class BaseSensorChartFragment extends Fragment {

    protected ChartViewModel viewModel;
    protected LinearLayout chartContainer;
    protected Button resetButton;

    protected LineChart xAxisChart;
    protected LineChart yAxisChart;
    protected LineChart zAxisChart;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(ChartViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_sensor_chart, container, false);

        // Find views
        chartContainer = view.findViewById(R.id.chart_container);
        resetButton = view.findViewById(R.id.reset_button);

        // Create charts
        xAxisChart = createChart("X-Axis Data");
        yAxisChart = createChart("Y-Axis Data");
        zAxisChart = createChart("Z-Axis Data");

        // Add charts to container
        chartContainer.addView(xAxisChart);
        chartContainer.addView(yAxisChart);
        chartContainer.addView(zAxisChart);

        // Set up reset button
        resetButton.setOnClickListener(v -> viewModel.resetChartData());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Add a TextView for debug information
        TextView debugText = new TextView(requireContext());
        debugText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        debugText.setPadding(16, 16, 16, 16);
        debugText.setTextColor(Color.RED);
        debugText.setText("Debug: Waiting for sensor data...");

        chartContainer.addView(debugText, 0); // Add at the top

        // Update debug text with sensor connection status
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if we have any data
                    boolean hasAccelData = viewModel.getAccelerometerXData().getValue() != null &&
                            !viewModel.getAccelerometerXData().getValue().isEmpty();
                    boolean hasGyroData = viewModel.getGyroscopeXData().getValue() != null &&
                            !viewModel.getGyroscopeXData().getValue().isEmpty();
                    boolean hasMagData = viewModel.getMagnetometerXData().getValue() != null &&
                            !viewModel.getMagnetometerXData().getValue().isEmpty();

                    String status = "Debug: ";
                    if (hasAccelData) status += "Accel OK | ";
                    else status += "No Accel | ";

                    if (hasGyroData) status += "Gyro OK | ";
                    else status += "No Gyro | ";

                    if (hasMagData) status += "Mag OK";
                    else status += "No Mag";

                    debugText.setText(status);
                } catch (Exception e) {
                    debugText.setText("Debug: Error - " + e.getMessage());
                }

                // Run again in 2 seconds
                if (isAdded()) {
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);

        // Setup observations for data
        setupChartObservers();
    }

    /**
     * Create a line chart with common settings
     * @param description Chart description
     * @return Configured LineChart
     */
    protected LineChart createChart(String description) {
        LineChart chart = new LineChart(requireContext());

        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        params.setMargins(0, 0, 0, 16);
        chart.setLayoutParams(params);

        // Configure chart appearance
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(true);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setVisibleXRangeMaximum(50); // Show only 50 data points at a time
        chart.setKeepPositionOnRotation(true);

        // Set description
        Description desc = new Description();
        desc.setText(description);
        chart.setDescription(desc);

        // Configure X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(0.5f);

        // Configure Y axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawZeroLine(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Set empty data
        LineData data = new LineData();
        chart.setData(data);

        return chart;
    }

    /**
     * Update chart with new data
     * @param chart The chart to update
     * @param entries New data entries
     * @param label Data set label
     * @param color Line color
     */
    protected void updateChart(LineChart chart, List<ChartViewModel.Entry> entries, String label, int color) {
        if (chart == null || entries == null || entries.isEmpty()) {
            return;
        }

        // Convert entries to MPAndroidChart Entry objects
        List<Entry> chartEntries = new ArrayList<>();
        for (ChartViewModel.Entry entry : entries) {
            chartEntries.add(new Entry(entry.getX(), entry.getY()));
        }

        // Create or update dataset
        LineDataSet dataSet;
        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            dataSet = (LineDataSet) chart.getData().getDataSetByIndex(0);
            dataSet.setValues(chartEntries);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            dataSet = new LineDataSet(chartEntries, label);
            dataSet.setColor(color);
            dataSet.setCircleColor(color);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setLineWidth(2f);
            dataSet.setHighlightEnabled(true);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            // Create line data and set it to chart
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
        }

        // Move to latest data
        chart.moveViewToX(entries.get(entries.size() - 1).getX());

        // Refresh chart
        chart.invalidate();
    }

    /**
     * Setup chart observers for data changes
     */
    protected abstract void setupChartObservers();
}