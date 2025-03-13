package com.dylanlxlx.instameasure.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态步长估计器
 * 基于步频、加速度和用户特征估计步长
 */
public class DynamicStepLengthEstimator {
    // 步长参数
    private static final float DEFAULT_STEP_LENGTH = 0.7f; // 默认步长(米)
    private static final float MIN_STEP_LENGTH = 0.4f;     // 最小步长(米)
    private static final float MAX_STEP_LENGTH = 1.0f;     // 最大步长(米)

    // 用户参数
    private float userHeight = 1.7f;  // 默认身高(米)
    private float strideRatio = 0.41f; // 步长与身高比例(可根据用户校准)

    // 最近步数时间(用于计算步频)
    private List<Long> recentStepTimes = new ArrayList<>();
    private float lastStepLength = DEFAULT_STEP_LENGTH;

    // 步长校准
    private boolean isCalibrated = false;
    private float calibrationFactor = 1.0f;

    public DynamicStepLengthEstimator() {
        this(1.7f); // 默认身高
    }

    public DynamicStepLengthEstimator(float userHeight) {
        this.userHeight = userHeight;
    }

    /**
     * 估计当前步长
     * @param accelMagnitude 加速度幅值
     * @return 估计的步长(米)
     */
    public float estimateStepLength(float accelMagnitude) {
        // 记录步伐时间
        recordStepTime();

        // 静态步长 (基于身高)
        float staticStepLength = userHeight * strideRatio;

        // 根据加速度调整
        float accelFactor = calculateAccelFactor(accelMagnitude);

        // 根据步频调整
        float frequencyFactor = calculateFrequencyFactor();

        // 计算动态步长
        float dynamicStepLength = staticStepLength * accelFactor * frequencyFactor;

        // 步长平滑处理
        dynamicStepLength = smoothStepLength(dynamicStepLength);

        // 应用校准因子
        if (isCalibrated) {
            dynamicStepLength *= calibrationFactor;
        }

        // 确保步长在合理范围内
        lastStepLength = constrain(dynamicStepLength, MIN_STEP_LENGTH, MAX_STEP_LENGTH);
        return lastStepLength;
    }

    /**
     * 记录步伐时间
     */
    private void recordStepTime() {
        long currentTime = System.currentTimeMillis();
        recentStepTimes.add(currentTime);

        // 保留最近5个步伐时间
        if (recentStepTimes.size() > 5) {
            recentStepTimes.remove(0);
        }
    }

    /**
     * 计算加速度因子
     * 加速度越大，步长越大
     */
    private float calculateAccelFactor(float accelMagnitude) {
        // 正常行走加速度幅值约10m/s²
        float normalAccel = 10.0f;

        // 加速度影响系数(0.8-1.2)
        return constrain(accelMagnitude / normalAccel, 0.8f, 1.2f);
    }

    /**
     * 计算步频因子
     * 步频越快，步长越长(但过快会减小)
     */
    private float calculateFrequencyFactor() {
        float stepFrequency = calculateStepFrequency();
        if (stepFrequency <= 0) return 1.0f;

        // 正常步频约2步/秒
        float normalFrequency = 2.0f;

        // 缓慢走路(步频低)：步长较短
        // 正常行走(步频中等)：步长适中
        // 快速行走(步频稍高)：步长较长
        // 跑步(步频很高)：步长变短
        if (stepFrequency < normalFrequency) {
            // 缓慢行走至正常行走，步长线性增加
            return 0.85f + 0.15f * (stepFrequency / normalFrequency);
        } else if (stepFrequency < normalFrequency * 1.5) {
            // 正常行走至快速行走，步长增加
            return 1.0f + 0.2f * ((stepFrequency - normalFrequency) / normalFrequency);
        } else {
            // 跑步时步长反而减小
            return (float) (1.2f - 0.1f * ((stepFrequency - normalFrequency * 1.5) / normalFrequency));
        }
    }

    /**
     * 计算当前步频(步/秒)
     */
    private float calculateStepFrequency() {
        if (recentStepTimes.size() < 2) return 0;

        long timeSpan = recentStepTimes.get(recentStepTimes.size() - 1) -
                recentStepTimes.get(0);
        int stepCount = recentStepTimes.size() - 1;

        return timeSpan > 0 ? (stepCount * 1000.0f) / timeSpan : 0;
    }

    /**
     * 平滑步长变化
     */
    private float smoothStepLength(float newStepLength) {
        // 避免步长突变(最大变化15%)
        float maxChange = lastStepLength * 0.15f;
        float delta = newStepLength - lastStepLength;

        if (Math.abs(delta) > maxChange) {
            return lastStepLength + (delta > 0 ? maxChange : -maxChange);
        }
        return newStepLength;
    }

    /**
     * 通过已知距离校准步长
     * @param actualDistance 实际步行距离(米)
     * @param stepCount 步数
     */
    public void calibrate(float actualDistance, int stepCount) {
        if (stepCount <= 0) return;

        float measuredAverageStepLength = actualDistance / stepCount;
        calibrationFactor = measuredAverageStepLength / lastStepLength;

        // 确保校准因子在合理范围内
        calibrationFactor = constrain(calibrationFactor, 0.7f, 1.3f);
        isCalibrated = true;
    }

    /**
     * 设置用户身高
     */
    public void setUserHeight(float height) {
        this.userHeight = height;
    }

    /**
     * 约束值在范围内
     */
    private float constrain(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }
}