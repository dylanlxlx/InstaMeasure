package com.dylanlxlx.instameasure.utils;

import static com.dylanlxlx.instameasure.utils.MathUtils.calculateMagnitude;

import java.util.ArrayList;
import java.util.List;

/**
 * 步数检测器
 * 使用改进的算法检测步数
 */
public class StepDetector {
    // 常量定义
    private static final int RECENT_DIFF_BUFFER_SIZE = 5;    // 最近波峰波谷差值缓冲区大小
    private static final float STEP_THRESHOLD_BASE = 1.7f;   // 步数检测的基础阈值
    private static final float PEAK_MINIMUM = 9.5f;          // 波峰最小幅度
    private static final float PEAK_MAXIMUM = 20.0f;         // 波峰最大幅度
    private static final long MIN_STEP_INTERVAL = 200;       // 步数最小时间间隔(毫秒)
    private static final long MAX_STEP_INTERVAL = 2000;      // 步数最大时间间隔(毫秒)
    private static final int RECENT_ACCELERATION_SIZE = 50;  // 最近加速度缓冲区大小

    // 状态变量
    private float[] recentPeakValleyDifferences = new float[RECENT_DIFF_BUFFER_SIZE];
    private int differenceBufferPosition = 0;
    private boolean isTrendRising = false;
    private int risingStreak = 0;
    private int lastRisingStreak = 0;
    private boolean wasTrendRising = false;
    private float currentPeak = 0;
    private float currentValley = 0;
    private long timeOfCurrentPeak = 0;
    private long timeOfLastPeak = 0;
    private float previousValue = 0;
    private float activeThreshold = 2.0f;

    // 步数计数
    private int stepCount = 0;

    // 最近加速度数据(用于分析)
    private List<float[]> recentAccelerations = new ArrayList<>();
    private float[] lastAcceleration = null;

    // 行走状态
    private String walkingState = "STILL"; // STILL, WALKING, RUNNING
    private long lastStateUpdateTime = 0;

    // 回调接口
    public interface StepCallback {
        void onStepDetected(int stepCount);
        void onWalkingStateChanged(String newState);
    }

    private StepCallback callback;

    // 构造函数
    public StepDetector(StepCallback callback) {
        this.callback = callback;
    }

    // 处理传感器数据
    public void processSensorData(float[] values) {
        // 保存最近加速度
        lastAcceleration = values.clone();
        updateRecentAccelerations(values);

        // 分析行走状态
        analyzeWalkingState();

        // 计算三轴加速度的合成值
        float currentMagnitude = calculateMagnitude(values);
        analyzeStep(currentMagnitude);
    }

    // 分析并检测新的一步
    private void analyzeStep(float currentMagnitude) {
        if (identifyPeak(currentMagnitude, previousValue)) {
            timeOfLastPeak = timeOfCurrentPeak;
            long timeOfNow = System.currentTimeMillis();

            // 判断是否为一步：时间间隔和波峰波谷差值满足条件
            if (timeOfNow - timeOfLastPeak >= MIN_STEP_INTERVAL &&
                    currentPeak - currentValley >= activeThreshold &&
                    timeOfNow - timeOfLastPeak <= MAX_STEP_INTERVAL) {
                timeOfCurrentPeak = timeOfNow;
                stepCount++;
                if (callback != null) {
                    callback.onStepDetected(stepCount);
                }
            }

            // 如果波峰波谷差值大于基础阈值，则更新动态阈值
            if (timeOfNow - timeOfLastPeak >= MIN_STEP_INTERVAL &&
                    currentPeak - currentValley >= STEP_THRESHOLD_BASE) {
                timeOfCurrentPeak = timeOfNow;
                activeThreshold = updateActiveThreshold(currentPeak - currentValley);
            }
        }
        previousValue = currentMagnitude;
    }

    // 识别波峰
    private boolean identifyPeak(float newValue, float oldValue) {
        wasTrendRising = isTrendRising;
        if (newValue >= oldValue) {
            isTrendRising = true;
            risingStreak++;
        } else {
            lastRisingStreak = risingStreak;
            risingStreak = 0;
            isTrendRising = false;
        }

        // 判断波峰条件：趋势从上升变为下降，且上升连续次数>=2，峰值在合理范围内
        if (!isTrendRising && wasTrendRising &&
                lastRisingStreak >= 2 &&
                oldValue >= PEAK_MINIMUM && oldValue < PEAK_MAXIMUM) {
            currentPeak = oldValue;
            return true;
        } else if (!wasTrendRising && isTrendRising) {
            // 记录波谷
            currentValley = oldValue;
            return false;
        } else {
            return false;
        }
    }

    // 更新动态阈值
    private float updateActiveThreshold(float difference) {
        float tempThreshold = activeThreshold;
        if (differenceBufferPosition < RECENT_DIFF_BUFFER_SIZE) {
            recentPeakValleyDifferences[differenceBufferPosition] = difference;
            differenceBufferPosition++;
        } else {
            tempThreshold = computeGradientThreshold(recentPeakValleyDifferences, RECENT_DIFF_BUFFER_SIZE);
            System.arraycopy(recentPeakValleyDifferences, 1, recentPeakValleyDifferences, 0, RECENT_DIFF_BUFFER_SIZE - 1);
            recentPeakValleyDifferences[RECENT_DIFF_BUFFER_SIZE - 1] = difference;
        }
        return tempThreshold;
    }

    // 计算梯度化的阈值
    private float computeGradientThreshold(float[] differences, int size) {
        float sum = 0;
        for (int i = 0; i < size; i++) {
            sum += differences[i];
        }
        float averageDifference = sum / size;

        // 根据平均差值动态调整阈值
        if (averageDifference >= 8) {
            return 4.3f;
        } else if (averageDifference >= 7) {
            return 3.3f;
        } else if (averageDifference >= 4) {
            return 2.3f;
        } else if (averageDifference >= 3) {
            return 2.0f;
        } else {
            return 1.7f;
        }
    }

    /**
     * 保存最近加速度数据
     */
    private void updateRecentAccelerations(float[] accel) {
        recentAccelerations.add(accel.clone());

        // 限制缓冲区大小
        if (recentAccelerations.size() > RECENT_ACCELERATION_SIZE) {
            recentAccelerations.remove(0);
        }
    }

    /**
     * 分析行走状态
     * STILL: 静止
     * WALKING: 走路
     * RUNNING: 跑步
     */
    private void analyzeWalkingState() {
        if (recentAccelerations.size() < 10) return;

        long now = System.currentTimeMillis();
        if (now - lastStateUpdateTime < 1000) return; // 最多1秒更新一次状态

        // 计算加速度标准差
        float stdDev = calculateAccelStdDev();

        // 计算步频
        float stepFrequency = 0;
        if (timeOfCurrentPeak > 0 && timeOfLastPeak > 0) {
            long interval = timeOfCurrentPeak - timeOfLastPeak;
            if (interval > 0) {
                stepFrequency = 1000.0f / interval; // 步/秒
            }
        }

        // 根据加速度标准差和步频判断状态
        String newState;
        if (stdDev < 0.5) {
            newState = "STILL";
        } else if (stepFrequency > 2.5 || stdDev > 5.0) {
            newState = "RUNNING";
        } else {
            newState = "WALKING";
        }

        // 如果状态变化，通知回调
        if (!newState.equals(walkingState)) {
            walkingState = newState;
            if (callback != null) {
                callback.onWalkingStateChanged(walkingState);
            }
        }

        lastStateUpdateTime = now;
    }

    /**
     * 计算加速度标准差
     */
    private float calculateAccelStdDev() {
        if (recentAccelerations.size() < 2) return 0;

        // 计算平均幅值
        float sum = 0;
        for (float[] accel : recentAccelerations) {
            sum += calculateMagnitude(accel);
        }
        float mean = sum / recentAccelerations.size();

        // 计算方差
        float variance = 0;
        for (float[] accel : recentAccelerations) {
            float magnitude = calculateMagnitude(accel);
            variance += (magnitude - mean) * (magnitude - mean);
        }
        variance /= recentAccelerations.size();

        // 返回标准差
        return (float) Math.sqrt(variance);
    }

    /**
     * 获取当前步数
     */
    public int getStepCount() {
        return stepCount;
    }

    /**
     * 获取最近的加速度数据
     */
    public float[] getLastAcceleration() {
        return lastAcceleration;
    }

    /**
     * 获取当前行走状态
     */
    public String getWalkingState() {
        return walkingState;
    }

    /**
     * 重置步数检测器
     */
    public void reset() {
        stepCount = 0;
        differenceBufferPosition = 0;
        isTrendRising = false;
        risingStreak = 0;
        lastRisingStreak = 0;
        wasTrendRising = false;
        currentPeak = 0;
        currentValley = 0;
        timeOfCurrentPeak = 0;
        timeOfLastPeak = 0;
        previousValue = 0;
        activeThreshold = 2.0f;
        recentAccelerations.clear();
        walkingState = "STILL";
    }
}