package com.dylanlxlx.instameasure.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dylanlxlx.instameasure.data.repository.MeasurementRepository;
import com.dylanlxlx.instameasure.model.ArMeasurementResult;
import com.dylanlxlx.instameasure.model.MeasurementPoint;
import com.google.ar.core.Anchor;

/**
 * AR测量的ViewModel
 * 管理AR测量状态和数据
 */
public class ArMeasureViewModel extends ViewModel {
    // LiveData对象
    private final MutableLiveData<ArMeasurementResult> measurementResult;
    private final MutableLiveData<Boolean> isPlaneDetected;
    private final MutableLiveData<String> statusMessage;
    private final MutableLiveData<Boolean> isMeasuring;

    // 点ID计数器
    private int nextPointId = 0;

    // 自动闭合阈值(米)
    private static final float CLOSE_THRESHOLD = 0.2f;

    public ArMeasureViewModel() {
        measurementResult = new MutableLiveData<>(new ArMeasurementResult());
        isPlaneDetected = new MutableLiveData<>(false);
        statusMessage = new MutableLiveData<>("正在检测平面...");
        isMeasuring = new MutableLiveData<>(false);
    }

    /**
     * 添加测量点
     */
    public void addPoint(Anchor anchor) {
        if (!isMeasuring.getValue()) {
            startMeasuring();
        }

        ArMeasurementResult result = measurementResult.getValue();
        if (result != null) {
            // 创建新测量点
            MeasurementPoint point = new MeasurementPoint(nextPointId++, anchor);
            result.addPoint(point);

            // 更新LiveData
            measurementResult.setValue(result);

            // 检查是否需要自动闭合
            if (result.getPoints().size() >= 3) {
                boolean closed = result.tryAutoClose(CLOSE_THRESHOLD);
                if (closed) {
                    statusMessage.setValue("区域已自动闭合，面积: " +
                            String.format("%.2f", result.getArea()) + "平方米");
                } else {
                    updateStatusMessage(result.getPoints().size());
                }
            } else {
                updateStatusMessage(result.getPoints().size());
            }
        }
    }

    /**
     * 更新点的位置
     */
    public void updatePointPosition(int pointId, float[] position) {
        ArMeasurementResult result = measurementResult.getValue();
        if (result != null) {
            for (MeasurementPoint point : result.getPoints()) {
                if (point.getId() == pointId) {
                    point.updatePosition(position);
                    break;
                }
            }

            // 如果区域已闭合，重新计算
            if (result.isClosed()) {
                result.closeArea();
            }

            measurementResult.setValue(result);
        }
    }

    /**
     * 闭合测量区域
     */
    public void closeArea() {
        ArMeasurementResult result = measurementResult.getValue();
        if (result != null && result.getPoints().size() >= 3 && !result.isClosed()) {
            result.closeArea();
            measurementResult.setValue(result);

            // 更新状态消息
            statusMessage.setValue("区域已闭合，面积: " +
                    String.format("%.2f", result.getArea()) + "平方米");
        } else if (result != null && result.getPoints().size() < 3) {
            statusMessage.setValue("需要至少3个点才能闭合区域");
        }
    }

    /**
     * 开始测量
     */
    private void startMeasuring() {
        isMeasuring.setValue(true);
        statusMessage.setValue("点击添加第一个测量点");
    }

    /**
     * 重置测量
     */
    public void resetMeasurement() {
        ArMeasurementResult result = measurementResult.getValue();
        if (result != null) {
            result.reset();
            measurementResult.setValue(result);
        }

        nextPointId = 0;
        isMeasuring.setValue(false);
        statusMessage.setValue("已重置，点击添加新的测量点");
    }

    /**
     * 保存测量结果
     * @return 是否成功保存
     */
    public boolean saveMeasurement() {
        ArMeasurementResult result = measurementResult.getValue();
        if (result != null && result.getPoints().size() >= 2) {
            // 如果有3个及以上点但未闭合，尝试闭合
            if (result.getPoints().size() >= 3 && !result.isClosed()) {
                result.closeArea();
            }

            // 转换并保存到存储库
            MeasurementRepository.getInstance().saveMeasurement(result.toMeasurement());
            return true;
        }
        return false;
    }

    /**
     * 设置平面已检测状态
     */
    public void setPlaneDetected(boolean detected) {
        if (detected && !isPlaneDetected.getValue()) {
            statusMessage.setValue("检测到平面，点击添加测量点");
        }
        isPlaneDetected.setValue(detected);
    }

    /**
     * 更新状态消息
     */
    private void updateStatusMessage(int pointCount) {
        if (pointCount == 1) {
            statusMessage.setValue("已添加第一个点，点击添加第二个点");
        } else if (pointCount == 2) {
            // 获取最新距离
            ArMeasurementResult result = measurementResult.getValue();
            if (result != null && !result.getDistances().isEmpty()) {
                float distance = result.getDistances().get(0);
                statusMessage.setValue("距离: " + String.format("%.2f", distance) + "米");
            }
        } else {
            // 获取总距离
            ArMeasurementResult result = measurementResult.getValue();
            if (result != null) {
                statusMessage.setValue("总距离: " +
                        String.format("%.2f", result.getTotalDistance()) + "米" +
                        " | 点击闭合计算面积");
            }
        }
    }

    // Getters
    public LiveData<ArMeasurementResult> getMeasurementResult() {
        return measurementResult;
    }

    public LiveData<Boolean> isPlaneDetected() {
        return isPlaneDetected;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Boolean> isMeasuring() {
        return isMeasuring;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // 清理AR资源
        ArMeasurementResult result = measurementResult.getValue();
        if (result != null) {
            result.reset();
        }
    }
}