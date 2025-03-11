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
import androidx.lifecycle.Observer;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.service.SensorService;
import com.dylanlxlx.instameasure.view.component.StepView2;

public class TempActivity2 extends AppCompatActivity {

    private TextView mStepText;
    private TextView mOrientText;
    private StepView2 mStepView;
    private SensorService sensorService;

    private int mStepLen = 50; // 步长

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_temp2);
        mStepText = findViewById(R.id.step_text);
        mOrientText = findViewById(R.id.orient_text);
        mStepView = findViewById(R.id.step_surfaceView);
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
        sensorService.getStepCountLiveData().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                mStepText.setText("步数:" + integer);
                mStepView.autoAddPoint(mStepLen);
            }
        });

        sensorService.getOrientationLiveData().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                mOrientText.setText("方向:" + aFloat);
                mStepView.autoDrawArrow(aFloat);
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