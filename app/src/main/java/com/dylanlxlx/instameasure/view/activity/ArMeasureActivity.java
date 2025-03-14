package com.dylanlxlx.instameasure.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.model.ArMeasurementResult;
import com.dylanlxlx.instameasure.model.MeasurementPoint;
import com.dylanlxlx.instameasure.view.component.MeasurementOverlayView;
import com.dylanlxlx.instameasure.viewmodel.ArMeasureViewModel;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import android.graphics.PointF;

/**
 * AR测量活动
 * 使用ARCore进行测距和面积计算
 */
public class ArMeasureActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private ArFragment arFragment;
    private ArMeasureViewModel viewModel;
    private MeasurementOverlayView overlayView;
    private TextView statusTextView;
    private Button btnClose, btnReset, btnSave;

    // 节点和渲染对象
    private List<Node> lineNodes = new ArrayList<>();
    private Renderable sphereRenderable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_measure);

        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(ArMeasureViewModel.class);

        // 找到视图
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
        overlayView = findViewById(R.id.measurement_overlay);
        statusTextView = findViewById(R.id.tv_status);
        btnClose = findViewById(R.id.btn_close);
        btnReset = findViewById(R.id.btn_reset);
        btnSave = findViewById(R.id.btn_save);

        // 设置按钮点击监听
        setupButtons();

        // 初始化AR
        initializeAr();

        // 设置观察者
        setupObservers();
    }

    private void initializeAr() {
        // 创建球体渲染对象
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(1.0f, 0, 0))
                .thenAccept(material -> sphereRenderable = ShapeFactory.makeSphere(0.02f, new Vector3(0, 0, 0), material));

        // 设置平面检测模式
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);

        // 设置点击监听
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            // 只有在检测到平面后才能添加点
            if (viewModel.isPlaneDetected().getValue()) {
                // 创建锚点
                Anchor anchor = hitResult.createAnchor();
                viewModel.addPoint(anchor);

                // 创建视觉标记
                addAnchorNode(anchor);
            }
        });
    }

    private void setupObservers() {
        // 观察测量结果
        viewModel.getMeasurementResult().observe(this, result -> {
            overlayView.updateMeasurementResult(result);
            updateLines(result);
            updateScreenPoints(result);
        });

        // 观察状态消息
        viewModel.getStatusMessage().observe(this, message -> {
            statusTextView.setText(message);
        });

        // 观察平面检测状态
        viewModel.isPlaneDetected().observe(this, detected -> {
            if (detected) {
                // 启用按钮
                btnSave.setEnabled(true);
                btnReset.setEnabled(true);
            }
        });

        // 观察测量状态
        viewModel.isMeasuring().observe(this, measuring -> {
            btnClose.setEnabled(measuring && viewModel.getMeasurementResult().getValue().getPoints().size() >= 3);
        });
    }

    private void setupButtons() {
        // 闭合按钮
        btnClose.setOnClickListener(v -> {
            viewModel.closeArea();
        });

        // 重置按钮
        btnReset.setOnClickListener(v -> {
            resetMeasurement();
        });

        // 保存按钮
        btnSave.setOnClickListener(v -> {
            saveMeasurement();
        });

        // 初始禁用按钮
        btnClose.setEnabled(false);
        btnSave.setEnabled(false);
        btnReset.setEnabled(false);
    }

    private void resetMeasurement() {
        // 清理场景中的节点
        for (Node node : lineNodes) {
            node.setParent(null);
        }
        lineNodes.clear();

        // 清理所有锚点节点
        ArMeasurementResult result = viewModel.getMeasurementResult().getValue();
        if (result != null) {
            for (MeasurementPoint point : result.getPoints()) {
                if (point.getAnchorNode() != null) {
                    point.getAnchorNode().setParent(null);
                }
            }
        }

        // 重置ViewModel
        viewModel.resetMeasurement();
    }

    private void saveMeasurement() {
        boolean success = viewModel.saveMeasurement();
        if (success) {
            Toast.makeText(this, "测量结果已保存", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "保存失败，需要至少两个测量点", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 添加锚点节点到场景
     */
    private void addAnchorNode(Anchor anchor) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // 创建可变换节点并添加渲染对象
        TransformableNode pointNode = new TransformableNode(arFragment.getTransformationSystem());
        pointNode.setParent(anchorNode);
        pointNode.setRenderable(sphereRenderable);

        // 更新测量点的锚点节点
        ArMeasurementResult result = viewModel.getMeasurementResult().getValue();
        if (result != null && !result.getPoints().isEmpty()) {
            MeasurementPoint lastPoint = result.getPoints().get(result.getPoints().size() - 1);
            lastPoint.setAnchorNode(anchorNode);
            // 获取并设置世界坐标
            lastPoint.updatePositionFromAnchorNode();

            // 添加到线节点列表
            lineNodes.add(pointNode);
        }
    }

    /**
     * 更新线条连接
     */
    private void updateLines(ArMeasurementResult result) {
        // 在这里我们主要依赖叠加视图绘制线条
        // 如果需要3D线条，可以在这里实现
    }

    /**
     * 更新测量点的屏幕坐标
     */
    private void updateScreenPoints(ArMeasurementResult result) {
        if (result == null || result.getPoints().isEmpty()) {
            return;
        }

        List<PointF> screenPoints = new ArrayList<>();

        for (MeasurementPoint point : result.getPoints()) {
            if (point.getAnchorNode() != null) {
                // 将3D坐标转换为屏幕坐标
                Vector3 worldPosition = point.getAnchorNode().getWorldPosition();

                // 更新点的3D位置
                float[] position = new float[]{worldPosition.x, worldPosition.y, worldPosition.z};
                point.updatePosition(position);

                // 转换为屏幕坐标
                PointF screenPoint = getScreenCoordinates(worldPosition);
                if (screenPoint != null) {
                    screenPoints.add(screenPoint);
                }
            }
        }

        // 更新叠加视图中的屏幕点
        overlayView.updateScreenPoints(screenPoints);
    }

    /**
     * 将3D世界坐标转换为屏幕坐标
     */
    private PointF getScreenCoordinates(Vector3 worldPosition) {
        ArSceneView sceneView = arFragment.getArSceneView();

        float[] projectionMatrix = new float[16];
        float[] viewMatrix = new float[16];

        sceneView.getArFrame().getCamera().getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);
        sceneView.getArFrame().getCamera().getViewMatrix(viewMatrix, 0);

        float[] viewProjectionMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        float[] worldCoordinates = new float[]{worldPosition.x, worldPosition.y, worldPosition.z, 1.0f};
        float[] screenCoordinates = new float[4];

        android.opengl.Matrix.multiplyMV(screenCoordinates, 0, viewProjectionMatrix, 0, worldCoordinates, 0);

        if (screenCoordinates[3] != 0) {
            screenCoordinates[0] /= screenCoordinates[3];
            screenCoordinates[1] /= screenCoordinates[3];
        }

        // 坐标映射到屏幕
        float x = (screenCoordinates[0] + 1) * sceneView.getWidth() / 2;
        float y = (1 - screenCoordinates[1]) * sceneView.getHeight() / 2;

        return new PointF(x, y);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if (frame == null) {
            return;
        }

        // 检测平面
        Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);
        boolean hasPlane = false;

        for (Plane plane : planes) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                hasPlane = true;
                break;
            }
        }

        viewModel.setPlaneDetected(hasPlane);

        // 更新锚点和屏幕点
        ArMeasurementResult result = viewModel.getMeasurementResult().getValue();
        if (result != null && !result.getPoints().isEmpty()) {
            updateScreenPoints(result);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // 恢复AR会话
            arFragment.getArSceneView().resume();
        } catch (CameraNotAvailableException e) {
            Toast.makeText(this, "相机不可用：" + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停AR会话
        arFragment.getArSceneView().pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        resetMeasurement();
    }
}