package com.dylanlxlx.instameasure.view.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.dylanlxlx.instameasure.model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.List;

// 轨迹绘制
public class TrajectoryView extends View {
    private List<TrajectoryPoint> trajectory;
    private Paint paint;
    private Path path;
    private float scaleFactor = 1.0f;  // 缩放比例
    private float offsetX = 0f;        // X轴偏移
    private float offsetY = 0f;        // Y轴偏移
    private long lastDrawTime = 0;
    private static final long MIN_REDRAW_INTERVAL = 500; // 最小重绘间隔200ms

    public TrajectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        path = new Path();
    }

    // 外部传入轨迹数据
    public void setTrajectory(List<TrajectoryPoint> trajectory) {
        Log.d("TrajectoryView", "Received points: " + (trajectory != null ? trajectory.size() : 0));
        if (trajectory == null || trajectory.isEmpty()) {
            this.trajectory = new ArrayList<>();
            invalidate();
            return;
        }
        this.trajectory = trajectory;
//        calculateScaling(); // 计算缩放和偏移
//        invalidate();        // 触发重绘
        if (System.currentTimeMillis() - lastDrawTime > MIN_REDRAW_INTERVAL) {
            calculateScaling();
            invalidate();
            lastDrawTime = System.currentTimeMillis();
        }
    }

    // 计算缩放比例和偏移量
    private void calculateScaling() {
        if (trajectory == null || trajectory.isEmpty()) return;

        // 找到轨迹的最大/最小坐标
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (TrajectoryPoint p : trajectory) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        // 计算视图可用区域（留10%边距）
        float viewWidth = getWidth() * 0.9f;
        float viewHeight = getHeight() * 0.9f;

        // 计算缩放比例（取X/Y轴中更严格的缩放）
        float scaleX = (float) (viewWidth / (maxX - minX));
        float scaleY = (float) (viewHeight / (maxY - minY));
        scaleFactor = Math.min(scaleX, scaleY);

        // 计算偏移量（居中显示）
        offsetX = (float) (-minX * scaleFactor) + (getWidth() - (float) (maxX - minX) * scaleFactor) / 2;
        offsetY = (float) (-minY * scaleFactor) + (getHeight() - (float) (maxY - minY) * scaleFactor) / 2;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (trajectory == null || trajectory.isEmpty()) return;

        path.reset();
        boolean first = true;
        for (TrajectoryPoint p : trajectory) {
            float x = (float) p.getX() * scaleFactor + offsetX;
            float y = (float) p.getY() * scaleFactor + offsetY;

            // 转换为屏幕坐标系（Y轴向下）
            y = getHeight() - y;

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, paint);
    }
}
