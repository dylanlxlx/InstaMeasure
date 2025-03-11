package com.dylanlxlx.instameasure.utils;

public class KalmanFilter1D {
    private double x; // 当前状态估计值
    private double p; // 估计协方差
    private double q; // 过程噪声协方差
    private double r; // 测量噪声协方差

    /**
     * 构造函数，初始化滤波器参数
     * @param initialX 初始状态值
     * @param initialP 初始协方差
     * @param q 过程噪声协方差
     * @param r 测量噪声协方差
     */
    public KalmanFilter1D(double initialX, double initialP, double q, double r) {
        this.x = initialX;
        this.p = initialP;
        this.q = q;
        this.r = r;
    }

    /**
     * 对新测量值进行滤波
     * @param z 新测量值
     * @return 滤波后的状态估计值
     */
    public double filter(double z) {
        // 预测步骤
        double x_pred = x;           // 假设状态恒定
        double p_pred = p + q;       // 预测协方差增加过程噪声

        // 更新步骤
        double k = p_pred / (p_pred + r); // 计算卡尔曼增益
        x = x_pred + k * (z - x_pred);    // 更新状态估计
        p = (1 - k) * p_pred;             // 更新协方差

        return x;
    }
}