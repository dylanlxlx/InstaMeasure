package com.dylanlxlx.instameasure.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.service.SensorService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class AccelerometerFragment extends Fragment {
    private LineChart chartX;
    private LineChart chartY;
    private LineChart chartZ;
    private SensorService sensorService;

    // 通过构造方法传递SensorService
    public AccelerometerFragment(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accelerometer, container, false);

        // 初始化三个图表
        chartX = view.findViewById(R.id.chart_acc_x);
        chartY = view.findViewById(R.id.chart_acc_y);
        chartZ = view.findViewById(R.id.chart_acc_z);

        // 观察传感器数据
        observeSensorData();
        return view;
    }

    private void observeSensorData() {
        sensorService.getAccelerometerLiveData().observe(getViewLifecycleOwner(), values -> {
            addEntryToChart(chartX, values[0], "X轴");
            addEntryToChart(chartY, values[1], "Y轴");
            addEntryToChart(chartZ, values[2], "Z轴");
        });
    }

    private void addEntryToChart(LineChart chart, float value, String label) {
        LineData data = chart.getData();
        if (data == null) {
            data = new LineData();
            chart.setData(data);
            LineDataSet set = new LineDataSet(null, label);
            set.setDrawCircles(false); // 不绘制数据点圆圈
            set.setLineWidth(2f);     // 线宽
            data.addDataSet(set);
        }
        data.addEntry(new Entry(data.getEntryCount(), value), 0);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate(); // 刷新图表
    }
}