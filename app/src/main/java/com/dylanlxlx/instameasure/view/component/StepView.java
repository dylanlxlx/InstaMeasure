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

public class StepView extends View {
    private Paint mPaint;
    private Paint mStrokePaint;
    private Path mArrowPath; // 箭头路径

    private int cR = 10; // 圆点半径
    private int arrowR = 20; // 箭头半径

    private float mCurX = 200;
    private float mCurY = 200;
    private float mOrient;
    private Bitmap mBitmap;
    private List<PointF> mPointList = new ArrayList<>();

    public StepView(Context context) {
        this(context, null);
    }

    public StepView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StepView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        // 其余绘制逻辑
        for (PointF p : mPointList) {
            canvas.drawCircle(p.x, p.y, cR, mPaint);
        }
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
        invalidate();
    }

    public void autoDrawArrow(float orient) {
        mOrient = orient;
        invalidate();
    }
}