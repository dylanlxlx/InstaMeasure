package com.dylanlxlx.instameasure.view.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.dylanlxlx.instameasure.model.ArMeasurementResult;
import com.dylanlxlx.instameasure.model.MeasurementPoint;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 测量叠加视图
 * 在AR视图上绘制测量点、线段、距离标签和面积
 */
public class MeasurementOverlayView extends View {
    // 绘图工具
    private final Paint pointPaint;
    private final Paint linePaint;
    private final Paint textPaint;
    private final Paint areaPaint;
    private final DecimalFormat decimalFormat;

    // 当前测量结果
    private ArMeasurementResult measurementResult;

    // 屏幕坐标
    private final List<PointF> screenPoints = new ArrayList<>();

    public MeasurementOverlayView(Context context) {
        this(context, null);
    }

    public MeasurementOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MeasurementOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化绘图工具
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.YELLOW);
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setShadowLayer(5f, 0f, 0f, Color.BLACK);

        areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        areaPaint.setColor(Color.parseColor("#4400FF00"));
        areaPaint.setStyle(Paint.Style.FILL);

        decimalFormat = new DecimalFormat("0.00");
    }

    /**
     * 更新测量结果
     */
    public void updateMeasurementResult(ArMeasurementResult result) {
        this.measurementResult = result;
        invalidate();
    }

    /**
     * 更新点的屏幕坐标
     */
    public void updateScreenPoints(List<PointF> points) {
        screenPoints.clear();
        screenPoints.addAll(points);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (measurementResult == null || screenPoints.isEmpty()) {
            return;
        }

        // 绘制线段和距离标签
        for (int i = 0; i < screenPoints.size() - 1; i++) {
            PointF p1 = screenPoints.get(i);
            PointF p2 = screenPoints.get(i + 1);

            // 绘制线段
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint);

            // 在线段中间绘制距离标签
            if (measurementResult.getDistances().size() > i) {
                float distance = measurementResult.getDistances().get(i);
                String distanceText = decimalFormat.format(distance) + "m";

                float midX = (p1.x + p2.x) / 2;
                float midY = (p1.y + p2.y) / 2;

                // 计算文本偏移，避免遮挡线段
                float angle = (float) Math.atan2(p2.y - p1.y, p2.x - p1.x);
                float offset = 20f;
                float offsetX = (float) Math.sin(angle) * offset;
                float offsetY = (float) -Math.cos(angle) * offset;

                canvas.drawText(distanceText, midX + offsetX, midY + offsetY, textPaint);
            }
        }

        // 如果已闭合区域，绘制封闭路径和面积标签
        if (measurementResult.isClosed() && screenPoints.size() >= 3) {
            // 绘制最后一条连接第一个点的线
            PointF firstPoint = screenPoints.get(0);
            PointF lastPoint = screenPoints.get(screenPoints.size() - 1);
            canvas.drawLine(lastPoint.x, lastPoint.y, firstPoint.x, firstPoint.y, linePaint);

            // 绘制最后一段距离标签
            if (measurementResult.getDistances().size() >= screenPoints.size()) {
                float distance = measurementResult.getDistances().get(measurementResult.getDistances().size() - 1);
                String distanceText = decimalFormat.format(distance) + "m";

                float midX = (firstPoint.x + lastPoint.x) / 2;
                float midY = (firstPoint.y + lastPoint.y) / 2;

                float angle = (float) Math.atan2(firstPoint.y - lastPoint.y, firstPoint.x - lastPoint.x);
                float offset = 20f;
                float offsetX = (float) Math.sin(angle) * offset;
                float offsetY = (float) -Math.cos(angle) * offset;

                canvas.drawText(distanceText, midX + offsetX, midY + offsetY, textPaint);
            }

            // 绘制半透明多边形
            Path areaPath = new Path();
            areaPath.moveTo(screenPoints.get(0).x, screenPoints.get(0).y);

            for (int i = 1; i < screenPoints.size(); i++) {
                areaPath.lineTo(screenPoints.get(i).x, screenPoints.get(i).y);
            }

            areaPath.close();
            canvas.drawPath(areaPath, areaPaint);

            // 在中心绘制面积标签
            String areaText = decimalFormat.format(measurementResult.getArea()) + "m²";
            float[] centroid = calculateCentroid(screenPoints);

            canvas.drawText(areaText, centroid[0] - textPaint.measureText(areaText) / 2, centroid[1], textPaint);
        }

        // 绘制测量点
        for (int i = 0; i < screenPoints.size(); i++) {
            PointF p = screenPoints.get(i);

            // 第一个点使用绿色，其他点使用红色
            pointPaint.setColor(i == 0 ? Color.GREEN : Color.RED);
            canvas.drawCircle(p.x, p.y, 15, pointPaint);
        }
    }

    /**
     * 计算多边形中心点
     */
    private float[] calculateCentroid(List<PointF> points) {
        float sumX = 0;
        float sumY = 0;

        for (PointF point : points) {
            sumX += point.x;
            sumY += point.y;
        }

        return new float[]{sumX / points.size(), sumY / points.size()};
    }
}