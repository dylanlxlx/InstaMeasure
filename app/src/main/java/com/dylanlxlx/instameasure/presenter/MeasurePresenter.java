package com.dylanlxlx.instameasure.presenter;

import com.dylanlxlx.instameasure.contract.MeasureContract;

public class MeasurePresenter implements MeasureContract.Presenter {

    private MeasureContract.View view;

    public MeasurePresenter(MeasureContract.View view) {
        this.view = view;
    }

    @Override
    public void onStepUpdated(int stepCount) {
        if (view != null) {
            view.showStepCount(stepCount);
        }
    }

    @Override
    public void onOrientationUpdated(float orientation) {
        if (view != null) {
            view.showOrientation(orientation);
        }
    }

    // 后续添加其他业务逻辑方法
}
