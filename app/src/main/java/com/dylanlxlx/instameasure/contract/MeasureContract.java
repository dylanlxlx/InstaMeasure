package com.dylanlxlx.instameasure.contract;

public interface MeasureContract {
    interface View {
        /**
         * 显示步数
         * @param stepCount 当前步数
         */
        void showStepCount(int stepCount);

        /**
         * 显示方向信息
         * @param orientation 当前方向（单位：度）
         */
        void showOrientation(float orientation);

        // 可根据需要增加其他更新 UI 的方法
    }

    interface Presenter {
        void onStepUpdated(int stepCount);
        void onOrientationUpdated(float orientation);
        // 可根据需要增加其他业务处理方法
    }
}
