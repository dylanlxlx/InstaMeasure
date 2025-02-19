package com.dylanlxlx.instameasure.view.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.service.SensorService;
import com.dylanlxlx.instameasure.view.component.TrajectoryView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView tvDistance, tvArea;
    private SensorService sensorService;

    private TrajectoryView trajectoryView;
    private LiveData<List<TrajectoryPoint>> trajectoryLiveData;
    List<TrajectoryPoint> testData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        tvDistance = findViewById(R.id.tv_distance);
        tvArea = findViewById(R.id.tv_area);
        trajectoryView = findViewById(R.id.trajectory_view);

        // 启动传感器服务
        startService(new Intent(this, SensorService.class));
        bindService(new Intent(this, SensorService.class), serviceConnection, BIND_AUTO_CREATE);

        requestPermissions();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            sensorService = binder.getService();
            observeSensorData();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sensorService = null;
        }
    };

    private void observeSensorData() {
        sensorService.getCurrentDistance().observe(this, new Observer<Double>() {
            @Override
            public void onChanged(Double distance) {
                tvDistance.setText(String.format("距离: %.2f 米", distance));
            }
        });

        sensorService.getCurrentArea().observe(this, new Observer<Double>() {
            @Override
            public void onChanged(Double area) {
                tvArea.setText(String.format("面积: %.2f 平方米", area));
            }
        });

        trajectoryLiveData = sensorService.getTrajectoryLiveData();
        trajectoryLiveData.observe(this, new Observer<List<TrajectoryPoint>>() {
            @Override
            public void onChanged(List<TrajectoryPoint> trajectory) {
                trajectoryView.setTrajectory(trajectory);
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 100 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }
}