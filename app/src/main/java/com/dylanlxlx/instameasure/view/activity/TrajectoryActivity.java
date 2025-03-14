package com.dylanlxlx.instameasure.view.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.model.TrajectoryPoint;
import com.dylanlxlx.instameasure.service.LocationService;
import com.dylanlxlx.instameasure.service.SensorService;
import com.dylanlxlx.instameasure.view.component.TrajectoryView;
import com.dylanlxlx.instameasure.viewmodel.MeasureViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.ar.core.ArCoreApk;

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
    private TextView txtStepLength;
    private TextView txtGpsStatus;
    private TextView txtLocationAccuracy;
    private TextView txtSatelliteCount;
    private TextView txtLocatingMode;
    private Button btnStartStop;
    private Button btnModePdr;
    private Button btnModeGps;
    private Button btnModeHybrid;
    private FloatingActionButton fabSettings;
    private FloatingActionButton fabCalibrate;
    private FloatingActionButton fabArMeasure;
    private TrajectoryView trajectoryView;

    // 服务相关
    private SensorService sensorService;
    private LocationService locationService;
    private boolean isSensorServiceBound = false;
    private boolean isLocationServiceBound = false;

    // 状态控制
    private boolean isMeasuring = false;

    // 用户配置
    private float userHeight = 1.7f; // 默认用户身高(米)
    private String currentMode = "PDR"; // 当前定位模式(PDR/GPS/Hybrid)
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

        // 载入用户设置
        loadUserSettings();

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

        // 设置用户身高
        viewModel.setUserHeight(userHeight);

        // 设置初始定位模式
        setLocationMode(currentMode);

        // 尝试第二次绑定服务（有时第一次绑定可能不成功）
        btnStartStop.postDelayed(this::retryBindServices, 2000);
    }

    private void loadUserSettings() {
        SharedPreferences prefs = getSharedPreferences("InstaMeasureSettings", Context.MODE_PRIVATE);
        userHeight = prefs.getFloat("userHeight", 1.7f);
        currentMode = prefs.getString("locationMode", "PDR");
    }

    private void saveUserSettings() {
        SharedPreferences prefs = getSharedPreferences("InstaMeasureSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("userHeight", userHeight);
        editor.putString("locationMode", currentMode);
        editor.apply();
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
        txtStepLength = findViewById(R.id.txt_step_length);
        txtGpsStatus = findViewById(R.id.txt_gps_status);
        txtLocationAccuracy = findViewById(R.id.txt_location_accuracy);
        txtSatelliteCount = findViewById(R.id.txt_satellite_count);
        txtLocatingMode = findViewById(R.id.txt_locating_mode);
        btnStartStop = findViewById(R.id.btn_start_stop);
        btnModePdr = findViewById(R.id.btn_mode_pdr);
        btnModeGps = findViewById(R.id.btn_mode_gps);
        btnModeHybrid = findViewById(R.id.btn_mode_hybrid);
        fabSettings = findViewById(R.id.fab_settings);
        fabCalibrate = findViewById(R.id.fab_calibrate);
        fabArMeasure = findViewById(R.id.fab_ar_measure);
        trajectoryView = findViewById(R.id.trajectory_view);

        // 设置按钮点击事件
        btnStartStop.setOnClickListener(v -> toggleMeasurement());

        // 设置定位模式按钮
        btnModePdr.setOnClickListener(v -> setLocationMode("PDR"));
        btnModeGps.setOnClickListener(v -> setLocationMode("GPS"));
        btnModeHybrid.setOnClickListener(v -> setLocationMode("Hybrid"));

        // 设置悬浮按钮点击事件
        fabSettings.setOnClickListener(v -> showSettingsDialog());
        fabCalibrate.setOnClickListener(v -> showCalibrateDialog());
        fabArMeasure.setOnClickListener(v -> startArMeasurement());

        // 更新定位模式UI
        updateLocationModeUI();
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
        viewModel.getStepCount().observe(this, stepCount -> txtStepCount.setText(String.format("步数: %d", stepCount)));

        // 观察方向
        viewModel.getOrientation().observe(this, orientation -> txtOrientation.setText(String.format("方向: %s°", decimalFormat.format(orientation))));

        // 观察步长
        viewModel.getStepLength().observe(this, stepLength -> {
            txtStepLength.setText(String.format("步长: %s米", decimalFormat.format(stepLength)));

            // 当测量中且有新步数时，根据步长和方向更新位置
            if (isMeasuring && isLocationServiceBound) {
                Float orientation = viewModel.getOrientation().getValue();
                if (orientation != null) {
                    viewModel.addPosition(stepLength, orientation);
                }
            }
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
        viewModel.getMeasuredArea().observe(this, area -> txtArea.setText(String.format("面积: %s平方米", decimalFormat.format(area))));

        // 观察测量状态
        viewModel.getIsMeasuring().observe(this, measuring -> {
            isMeasuring = measuring;
            updateStartStopButton();

            // 启用/禁用按钮
            btnModePdr.setEnabled(!measuring);
            btnModeGps.setEnabled(!measuring);
            btnModeHybrid.setEnabled(!measuring);
            fabSettings.setEnabled(!measuring);
            fabCalibrate.setEnabled(!measuring);
        });

        // 观察GPS可用性
        viewModel.isGpsAvailable().observe(this, available -> {
            txtGpsStatus.setText(String.format("GPS: %s", available ? "可用" : "不可用"));
            btnModeGps.setEnabled(available);
            btnModeHybrid.setEnabled(available);
        });

        // 观察GPS精度
        viewModel.getGpsAccuracy().observe(this, accuracy -> txtLocationAccuracy.setText(String.format("精度: %s米", decimalFormat.format(accuracy))));

        // 观察卫星数量
        viewModel.getSatelliteCount().observe(this, count -> txtSatelliteCount.setText(String.format("卫星: %d颗", count)));

        // 观察定位模式
        viewModel.getLocatingMode().observe(this, mode -> {
            txtLocatingMode.setText(String.format("模式: %s", mode));
            currentMode = mode;
            updateLocationModeUI();
        });

        // 观察位置精度
        viewModel.getLocationAccuracy().observe(this, accuracy -> txtLocationAccuracy.setText(String.format("精度: %s米", decimalFormat.format(accuracy))));
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

        // 启动相应的定位服务模式
        if (currentMode.equals("GPS") || currentMode.equals("Hybrid")) {
            locationService.startGpsTracking();
        }
    }

    private void stopMeasurement() {
        viewModel.stopMeasurement();

        // 停止GPS追踪（如果正在使用）
        if (isLocationServiceBound && (currentMode.equals("GPS") || currentMode.equals("Hybrid"))) {
            locationService.stopGpsTracking();
        }

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

                // 更新GPS状态
                updateGpsStatus();
            }
        });
    }

    private void updateGpsStatus() {
        if (isLocationServiceBound && locationService != null) {
            boolean gpsEnabled = locationService.isGpsEnabled();
            int satelliteCount = locationService.getSatelliteCount();

            // 更新UI
            txtGpsStatus.setText(String.format("GPS: %s", gpsEnabled ? "可用" : "不可用"));
            txtSatelliteCount.setText(String.format("卫星: %d颗", satelliteCount));

            // 更新按钮状态
            btnModeGps.setEnabled(gpsEnabled);
            btnModeHybrid.setEnabled(gpsEnabled);
        }
    }

    /**
     * 设置定位模式
     * @param mode 定位模式：PDR, GPS, Hybrid
     */
    private void setLocationMode(String mode) {
        currentMode = mode;

        // 更新ViewModel中的GPS使用状态
        viewModel.setUseGps(!mode.equals("PDR"));

        // 更新UI
        updateLocationModeUI();

        // 保存设置
        saveUserSettings();

        // 通知用户
        Toast.makeText(this, "定位模式已切换为: " + mode, Toast.LENGTH_SHORT).show();
    }

    private void updateLocationModeUI() {
        // 重置按钮样式
        btnModePdr.setAlpha(0.5f);
        btnModeGps.setAlpha(0.5f);
        btnModeHybrid.setAlpha(0.5f);

        // 设置当前模式按钮高亮
        switch (currentMode) {
            case "PDR":
                btnModePdr.setAlpha(1.0f);
                break;
            case "GPS":
                btnModeGps.setAlpha(1.0f);
                break;
            case "Hybrid":
                btnModeHybrid.setAlpha(1.0f);
                break;
        }
    }

    /**
     * 显示设置对话框
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);

        // 获取对话框控件
        Slider heightSlider = view.findViewById(R.id.slider_height);
        TextView heightText = view.findViewById(R.id.txt_height_value);

        // 设置当前身高
        heightSlider.setValue(userHeight * 100); // 转为厘米
        heightText.setText(String.format("%.0f厘米", userHeight * 100));

        // 设置滑块监听
        heightSlider.addOnChangeListener((slider, value, fromUser) -> heightText.setText(String.format("%.0f厘米", value)));

        builder.setTitle("设置")
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    // 更新身高设置
                    userHeight = heightSlider.getValue() / 100f; // 转回米
                    viewModel.setUserHeight(userHeight);
                    saveUserSettings();

                    Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示校准对话框
     */
    private void showCalibrateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_calibrate, null);

        // 获取对话框控件
        RadioButton radioStepLength = view.findViewById(R.id.radio_step_length);
        RadioButton radioDirection = view.findViewById(R.id.radio_direction);
        Button btnStartCalibration = view.findViewById(R.id.btn_start_calibration);

        // 默认选择步长校准
        radioStepLength.setChecked(true);

        final AlertDialog dialog = builder.setTitle("传感器校准")
                .setView(view)
                .setNegativeButton("关闭", null)
                .create();

        // 开始校准按钮点击事件
        btnStartCalibration.setOnClickListener(v -> {
            dialog.dismiss();

            if (radioStepLength.isChecked()) {
                // 启动步长校准流程
                startStepLengthCalibration();
            } else if (radioDirection.isChecked()) {
                // 启动方向校准流程
                startDirectionCalibration();
            }
        });

        dialog.show();
    }

    /**
     * 开始AR测量
     */
    private void startArMeasurement() {
        // 检查设备是否支持ARCore
        boolean isArCoreAvailable = ArCoreApk.getInstance().checkAvailability(this).isSupported();

        if (isArCoreAvailable) {
            Intent intent = new Intent(this, ArMeasureActivity.class);
            startActivity(intent);
        } else {
            // 如果不支持ARCore，显示提示对话框
            new AlertDialog.Builder(this)
                    .setTitle("AR功能不可用")
                    .setMessage("您的设备不支持AR功能。请使用支持ARCore的设备。")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    /**
     * 开始步长校准
     */
    private void startStepLengthCalibration() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_step_calibration, null);

        // 获取对话框控件
        TextView txtInstructions = view.findViewById(R.id.txt_instructions);
        Button btnStartWalking = view.findViewById(R.id.btn_start_walking);
        Button btnStopWalking = view.findViewById(R.id.btn_stop_walking);
        Slider distanceSlider = view.findViewById(R.id.slider_distance);
        TextView txtDistance = view.findViewById(R.id.txt_distance_value);
        Button btnSaveCalibration = view.findViewById(R.id.btn_save_calibration);

        // 初始化控件状态
        btnStopWalking.setEnabled(false);
        btnSaveCalibration.setEnabled(false);
        txtInstructions.setText("请沿直线走10米距离进行校准。\n点击开始行走按钮，步行后点击停止行走。");

        distanceSlider.setValue(10); // 默认10米
        txtDistance.setText("10.0米");

        distanceSlider.addOnChangeListener((slider, value, fromUser) -> txtDistance.setText(String.format("%.1f米", value)));

        // 临时测量状态变量
        final int[] initialStepCount = {0};
        final int[] finalStepCount = {0};

        final AlertDialog dialog = builder.setTitle("步长校准")
                .setView(view)
                .setCancelable(false)
                .create();

        // 开始行走按钮点击事件
        btnStartWalking.setOnClickListener(v -> {
            // 记录初始步数
            Integer currentSteps = viewModel.getStepCount().getValue();
            initialStepCount[0] = currentSteps != null ? currentSteps : 0;

            // 更新UI状态
            btnStartWalking.setEnabled(false);
            btnStopWalking.setEnabled(true);
            distanceSlider.setEnabled(false);
            txtInstructions.setText("正在记录步数...\n请沿直线行走，完成后点击停止行走。");

            // 开始测量
            viewModel.startMeasurement();
        });

        // 停止行走按钮点击事件
        btnStopWalking.setOnClickListener(v -> {
            // 记录最终步数
            Integer currentSteps = viewModel.getStepCount().getValue();
            finalStepCount[0] = currentSteps != null ? currentSteps : 0;

            // 停止测量
            viewModel.stopMeasurement();

            // 计算步数差
            int stepsDifference = finalStepCount[0] - initialStepCount[0];

            // 更新UI状态
            btnStopWalking.setEnabled(false);
            btnSaveCalibration.setEnabled(true);
            txtInstructions.setText(String.format("记录了%d步。\n请调整滑块设置实际行走距离，然后点击保存校准。", stepsDifference));
            distanceSlider.setEnabled(true);
        });

        // 保存校准按钮点击事件
        btnSaveCalibration.setOnClickListener(v -> {
            // 计算步数差
            int stepsDifference = finalStepCount[0] - initialStepCount[0];

            if (stepsDifference > 0) {
                // 获取设置的距离
                float distance = distanceSlider.getValue();

                // 进行校准
                viewModel.calibrateStepLength(distance, stepsDifference);

                Toast.makeText(this, "步长校准已保存", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                txtInstructions.setText("未检测到步数变化，请重新校准。");
                btnStartWalking.setEnabled(true);
                btnSaveCalibration.setEnabled(false);
            }
        });

        dialog.setOnCancelListener(dialog1 -> {
            // 确保在对话框取消时停止测量
            if (isMeasuring) {
                viewModel.stopMeasurement();
            }
        });

        dialog.show();
    }

    /**
     * 开始方向校准
     */
    private void startDirectionCalibration() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("方向校准")
                .setMessage("请将手机平放，缓慢转动一整圈(360度)。\n这将帮助校准地磁传感器。")
                .setPositiveButton("完成", (dialog, which) -> Toast.makeText(this, "方向已校准", Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
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

        // 保存用户设置
        saveUserSettings();
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