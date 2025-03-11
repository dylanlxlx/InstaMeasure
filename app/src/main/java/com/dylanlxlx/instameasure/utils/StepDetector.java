package com.dylanlxlx.instameasure.utils;

import static com.dylanlxlx.instameasure.utils.MathUtils.calculateMagnitude;

public class StepDetector {
    // 常量定义
    private static final int RECENT_DIFF_BUFFER_SIZE = 5;           // 最近波峰波谷差值缓冲区大小
    private static final float STEP_THRESHOLD_BASE = 1.7f;          // 步数检测的基础阈值
    private static final float PEAK_MINIMUM = 11f;                  // 波峰最小幅度（约1.2g）
    private static final float PEAK_MAXIMUM = 19.6f;                // 波峰最大幅度（约2g）
    private static final long MIN_STEP_INTERVAL = 200;              // 步数最小时间间隔（毫秒）
    private static final long MAX_STEP_INTERVAL = 2000;             // 步数最大时间间隔（毫秒）

    // 状态变量
    private float[] recentPeakValleyDifferences = new float[RECENT_DIFF_BUFFER_SIZE]; // 存储最近波峰波谷差值的缓冲区
    private int differenceBufferPosition = 0;                       // 差值缓冲区的当前填充位置
    private boolean isTrendRising = false;                          // 当前趋势是否为上升
    private int risingStreak = 0;                                   // 当前连续上升的次数
    private int lastRisingStreak = 0;                               // 上一个检测点的连续上升次数
    private boolean wasTrendRising = false;                         // 上一个检测点的趋势状态
    private float currentPeak = 0;                                  // 当前波峰值
    private float currentValley = 0;                                // 当前波谷值
    private long timeOfCurrentPeak = 0;                             // 当前波峰时间
    private long timeOfLastPeak = 0;                                // 上次波峰时间
    private float previousValue = 0;                                // 上次传感器值
    private float activeThreshold = 2.0f;                           // 当前生效的动态阈值

    // 步数计数
    private int stepCount = 0;

    // 回调接口，用于通知外部步数变化
    public interface StepCallback {
        void onStepDetected(int stepCount);
    }

    private StepCallback callback;

    // 构造函数
    public StepDetector(StepCallback callback) {
        this.callback = callback;
    }

    // 处理传感器数据
    public void processSensorData(float[] values) {
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

        // 判断波峰条件
        if (!isTrendRising && wasTrendRising &&
                lastRisingStreak >= 2 &&
                oldValue >= PEAK_MINIMUM && oldValue < PEAK_MAXIMUM) {
            currentPeak = oldValue;
            return true;
        } else if (!wasTrendRising && isTrendRising) {
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

    // 获取当前步数
    public int getStepCount() {
        return stepCount;
    }
}