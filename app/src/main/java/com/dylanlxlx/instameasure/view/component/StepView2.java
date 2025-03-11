package com.dylanlxlx.instameasure.view.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.dylanlxlx.instameasure.R;

import java.util.ArrayList;
import java.util.List;

public class StepView2 extends View {
    private Paint mPaint;
    private Paint mStrokePaint;
    private Path mArrowPath; // 箭头路径
    private Path mTrajectoryPath; // 轨迹路径

    private int cR = 10; // 圆点半径
    private int arrowR = 20; // 箭头半径

    private float mCurX = 200;
    private float mCurY = 200;
    private float mOrient;
    private Bitmap mBitmap;
    private List<PointF> mPointList = new ArrayList<>();

    public StepView2(Context context) {
        this(context, null);
    }

    public StepView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StepView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 初始化画笔
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mStrokePaint = new Paint(mPaint);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(5);

        // 初始化箭头路径
        mArrowPath = new Path();
        mArrowPath.arcTo(new RectF(-arrowR, -arrowR, arrowR, arrowR), 0, -180);
        mArrowPath.lineTo(0, -3 * arrowR);
        mArrowPath.close();

        // 初始化轨迹路径
        mTrajectoryPath = new Path();

        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) return;

        // 绘制 Bitmap
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight()),
                    new Rect(0, 0, getWidth(), getHeight()), null);
        } else {
            Log.w("CustomView", "mBitmap is null, using fallback");
            // 可选：绘制占位图
            // canvas.drawColor(Color.GRAY);
        }

        // 绘制轨迹线
        mStrokePaint.setColor(Color.RED); // 设置轨迹线的颜色
        canvas.drawPath(mTrajectoryPath, mStrokePaint);

        // 绘制箭头
        canvas.save();
        canvas.translate(mCurX, mCurY);
        canvas.rotate(mOrient);
        canvas.drawPath(mArrowPath, mPaint);
        canvas.drawArc(new RectF(-arrowR * 0.8f, -arrowR * 0.8f, arrowR * 0.8f, arrowR * 0.8f),
                0, 360, false, mStrokePaint);
        canvas.restore();
    }

    /**
     * 当屏幕被触摸时调用
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCurX = event.getX();
        mCurY = event.getY();
        mPointList.add(new PointF(mCurX, mCurY));
        updateTrajectoryPath();
        invalidate();
        return true;
    }

    /**
     * 自动增加点
     */
    public void autoAddPoint(float stepLen) {
        mCurX += (float) (stepLen * Math.sin(Math.toRadians(mOrient)));
        mCurY += -(float) (stepLen * Math.cos(Math.toRadians(mOrient)));
        mPointList.add(new PointF(mCurX, mCurY));
        updateTrajectoryPath();
        invalidate();
    }

    public void autoDrawArrow(float orient) {
        mOrient = orient;
        invalidate();
    }

    /**
     * 更新轨迹路径
     */
    private void updateTrajectoryPath() {
        mTrajectoryPath.reset();
        if (mPointList.size() > 0) {
            mTrajectoryPath.moveTo(mPointList.get(0).x, mPointList.get(0).y);
            for (int i = 1; i < mPointList.size(); i++) {
                mTrajectoryPath.lineTo(mPointList.get(i).x, mPointList.get(i).y);
            }
        }
    }
}