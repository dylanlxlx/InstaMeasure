package com.dylanlxlx.instameasure.view.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.service.LocationService;
import com.dylanlxlx.instameasure.service.SensorService;
import com.dylanlxlx.instameasure.view.component.TrajectoryView;
import com.dylanlxlx.instameasure.viewmodel.MeasureViewModel;

import java.text.DecimalFormat;
import java.util.List;

/**
 * 轨迹测量Activity
 * 显示用户行走的轨迹，计算轨迹长度和面积
 */
public class TrajectoryActivity extends AppCompatActivity {

    private MeasureViewModel viewModel;

    // UI元素
    private TextView txtStepCount;
    private TextView txtOrientation;
    private TextView txtDistance;
    private TextView txtArea;
    private Button btnStartStop;
    private TrajectoryView trajectoryView;

    // 服务相关
    private SensorService sensorService;
    private LocationService locationService;
    private boolean isSensorServiceBound = false;
    private boolean isLocationServiceBound = false;

    // 状态控制
    private boolean isMeasuring = false;
    private static final float STEP_LENGTH = 0.7f; // 默认步长（米）
    private static final float CLOSURE_THRESHOLD = 2.0f; // 轨迹封闭判定阈值（米）

    // 格式化工具
    private final DecimalFormat decimalFormat = new DecimalFormat("#0.00");

    // 服务连接
    private final ServiceConnection sensorServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            sensorService = binder.getService();
            isSensorServiceBound = true;
            updateButtonStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSensorServiceBound = false;
            updateButtonStatus();
        }
    };

    private final ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            isLocationServiceBound = true;
            updateButtonStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isLocationServiceBound = false;
            updateButtonStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trajectory);

        // 设置标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("轨迹测量");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(MeasureViewModel.class);

        // 初始化UI
        initViews();

        // 清除已有轨迹数据
        viewModel.clearTrajectoryPoints();

        // 绑定服务
        bindServices();

        // 设置观察者
        setupObservers();

        // 尝试第二次绑定服务（有时第一次绑定可能不成功）
        btnStartStop.postDelayed(this::retryBindServices, 2000);
    }

    private void retryBindServices() {
        if (!isSensorServiceBound || !isLocationServiceBound) {
            Toast.makeText(this, "正在重试连接服务...", Toast.LENGTH_SHORT).show();
            bindServices();
        }
    }

    private void initViews() {
        txtStepCount = findViewById(R.id.txt_step_count);
        txtOrientation = findViewById(R.id.txt_orientation);
        txtDistance = findViewById(R.id.txt_distance);
        txtArea = findViewById(R.id.txt_area);
        btnStartStop = findViewById(R.id.btn_start_stop);
        trajectoryView = findViewById(R.id.trajectory_view);

        // 设置按钮点击事件
        btnStartStop.setOnClickListener(v -> toggleMeasurement());
    }

    private void bindServices() {
        // 绑定传感器服务
        Intent sensorIntent = new Intent(this, SensorService.class);
        startService(sensorIntent);
        bindService(sensorIntent, sensorServiceConnection, Context.BIND_AUTO_CREATE);

        // 绑定位置服务
        Intent locationIntent = new Intent(this, LocationService.class);
        startService(locationIntent);
        bindService(locationIntent, locationServiceConnection, Context.BIND_AUTO_CREATE);

        // 设置一个短暂的延迟，确保服务已绑定
        btnStartStop.setEnabled(false);
        btnStartStop.setText("正在连接服务...");
        btnStartStop.postDelayed(() -> {
            btnStartStop.setEnabled(true);
            btnStartStop.setText("开始测量");
        }, 1500); // 1.5秒的延迟，通常足够服务绑定完成
    }

    private void setupObservers() {
        // 观察步数
        viewModel.getStepCount().observe(this, stepCount -> {
            txtStepCount.setText(String.format("步数: %d", stepCount));

            // 当测量中且有新步数时，根据步长和方向更新位置
            if (isMeasuring && isLocationServiceBound) {
                Float orientation = viewModel.getOrientation().getValue();
                if (orientation != null) {
                    viewModel.addPosition(STEP_LENGTH, orientation);
                }
            }
        });

        // 观察方向
        viewModel.getOrientation().observe(this, orientation -> {
            txtOrientation.setText(String.format("方向: %s°", decimalFormat.format(orientation)));
        });

        // 观察轨迹点
        viewModel.getTrajectoryPoints().observe(this, trajectoryPoints -> {
            // 更新轨迹视图
            trajectoryView.setTrajectoryPoints(trajectoryPoints);

            // 计算并显示轨迹长度
            float distance = calculateTrajectoryLength(trajectoryPoints);
            txtDistance.setText(String.format("距离: %s米", decimalFormat.format(distance)));

            // 检查轨迹是否封闭
            if (isMeasuring && trajectoryPoints.size() > 3) {
                checkTrajectoryEnclosure(trajectoryPoints);
            }
        });

        // 观察面积
        viewModel.getMeasuredArea().observe(this, area -> {
            txtArea.setText(String.format("面积: %s平方米", decimalFormat.format(area)));
        });

        // 观察测量状态
        viewModel.getIsMeasuring().observe(this, measuring -> {
            isMeasuring = measuring;
            updateStartStopButton();
        });
    }

    private void toggleMeasurement() {
        if (isMeasuring) {
            stopMeasurement();
        } else {
            startMeasurement();
        }
    }

    private void startMeasurement() {
        // 再次检查服务绑定状态
        if (!isSensorServiceBound) {
            // 尝试重新绑定传感器服务
            Intent sensorIntent = new Intent(this, SensorService.class);
            startService(sensorIntent);
            bindService(sensorIntent, sensorServiceConnection, Context.BIND_AUTO_CREATE);
            Toast.makeText(this, "正在连接传感器服务...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLocationServiceBound) {
            // 尝试重新绑定位置服务
            Intent locationIntent = new Intent(this, LocationService.class);
            startService(locationIntent);
            bindService(locationIntent, locationServiceConnection, Context.BIND_AUTO_CREATE);
            Toast.makeText(this, "正在连接位置服务...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清除轨迹视图中的旧轨迹
        trajectoryView.clearTrajectory();

        // 开始新的测量
        viewModel.startMeasurement();
        Toast.makeText(this, "开始测量轨迹", Toast.LENGTH_SHORT).show();
    }

    private void stopMeasurement() {
        viewModel.stopMeasurement();
        Toast.makeText(this, "停止测量轨迹", Toast.LENGTH_SHORT).show();
    }

    private void updateStartStopButton() {
        btnStartStop.setText(isMeasuring ? "停止测量" : "开始测量");
    }

    private void updateButtonStatus() {
        boolean servicesReady = isSensorServiceBound && isLocationServiceBound;

        runOnUiThread(() -> {
            if (!isMeasuring) {
                btnStartStop.setEnabled(servicesReady);
                btnStartStop.setText(servicesReady ? "开始测量" : "正在连接服务...");
            }

            if (servicesReady && locationService != null && sensorService != null) {
                Toast.makeText(this, "传感器服务已连接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 计算轨迹总长度
     * @param points 轨迹点列表
     * @return 轨迹长度（米）
     */
    private float calculateTrajectoryLength(List<TrajectoryPoint> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }

        float totalDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            TrajectoryPoint p1 = points.get(i - 1);
            TrajectoryPoint p2 = points.get(i);

            // 计算两点之间的欧氏距离
            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();
            totalDistance += Math.sqrt(dx * dx + dy * dy);
        }

        return totalDistance;
    }

    /**
     * 检查轨迹是否封闭
     * @param points 轨迹点列表
     */
    private void checkTrajectoryEnclosure(List<TrajectoryPoint> points) {
        if (points.size() < 5) {  // 至少需要5个点才能形成有意义的封闭轨迹
            return;
        }

        TrajectoryPoint firstPoint = points.get(0);
        TrajectoryPoint lastPoint = points.get(points.size() - 1);

        // 计算起点和终点之间的距离
        double dx = lastPoint.getX() - firstPoint.getX();
        double dy = lastPoint.getY() - firstPoint.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 如果距离小于阈值，认为轨迹已封闭
        if (distance < CLOSURE_THRESHOLD) {
            // 确保有一个闭合的轨迹点
            if (Math.abs(lastPoint.getX() - firstPoint.getX()) > 0.1 ||
                    Math.abs(lastPoint.getY() - firstPoint.getY()) > 0.1) {
                // 手动添加一个回到起点的轨迹点
                viewModel.addTrajectoryPoint(firstPoint.getX(), firstPoint.getY());
            }

            // 停止测量
            stopMeasurement();
            Toast.makeText(this, "轨迹已封闭，自动停止测量", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解绑服务
        if (isSensorServiceBound) {
            unbindService(sensorServiceConnection);
            isSensorServiceBound = false;
        }

        if (isLocationServiceBound) {
            unbindService(locationServiceConnection);
            isLocationServiceBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}