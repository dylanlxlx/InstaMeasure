package com.dylanlxlx.instameasure.view.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;

import com.dylanlxlx.instameasure.model.TrajectoryPoint;

import java.util.List;

/**
 * 轨迹显示视图
 * 自定义View用于绘制轨迹
 */
public class TrajectoryView extends View {
    private List<TrajectoryPoint> trajectoryPoints;
    private final Paint pathPaint = new Paint();
    private final Paint pointPaint = new Paint();
    private final Paint startPointPaint = new Paint();
    private final Paint arrowPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Path trajectoryPath = new Path();
    private final Path arrowPath = new Path();

    // 缩放和平移参数
    private float scale = 20f;  // 每米在屏幕上的像素数
    private float initialScale = 20f; // 初始缩放比例
    private float offsetX = 0;  // X轴偏移量
    private float offsetY = 0;  // Y轴偏移量
    private boolean firstDraw = true;
    private boolean hasTrajectory = false;

    // 网格参数
    private static final float GRID_SIZE = 1.0f; // 网格大小（米）
    private static final boolean SHOW_GRID = true; // 是否显示网格

    public TrajectoryView(Context context) {
        super(context);
        initPaints();
    }

    public TrajectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public TrajectoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        // 轨迹路径画笔
        pathPaint.setColor(Color.rgb(66, 133, 244)); // Google蓝
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(8);
        pathPaint.setAntiAlias(true);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        // 轨迹点画笔
        pointPaint.setColor(Color.rgb(219, 68, 55)); // Google红
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        // 起点画笔
        startPointPaint.setColor(Color.rgb(15, 157, 88)); // Google绿
        startPointPaint.setStyle(Paint.Style.FILL);
        startPointPaint.setAntiAlias(true);

        // 箭头画笔
        arrowPaint.setColor(Color.rgb(66, 133, 244)); // Google蓝
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);

        // 网格画笔
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
        gridPaint.setAntiAlias(true);
    }

    public void setTrajectoryPoints(List<TrajectoryPoint> points) {
        this.trajectoryPoints = points;
        hasTrajectory = points != null && !points.isEmpty();
        firstDraw = true;  // 重置firstDraw标志以重新计算缩放和偏移
        invalidate();      // 重绘视图
    }

    /**
     * 清除轨迹数据
     */
    public void clearTrajectory() {
        trajectoryPoints = null;
        hasTrajectory = false;
        firstDraw = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 保存画布状态
        canvas.save();

        // 将坐标系原点移动到视图中心
        canvas.translate(getWidth() / 2f, getHeight() / 2f);

        // 绘制网格（仅在轨迹存在时）
        if (SHOW_GRID && hasTrajectory) {
            drawGrid(canvas);
        }

        // 如果没有轨迹，绘制初始状态
        if (!hasTrajectory) {
            drawInitialState(canvas);
            canvas.restore();
            return;
        }

        // 如果是第一次绘制，则计算合适的缩放和偏移
        if (firstDraw) {
            calculateScaleAndOffset();
            firstDraw = false;
        }

        // 应用偏移
        canvas.translate(offsetX, offsetY);

        // 绘制轨迹路径
        drawTrajectoryPath(canvas);

        // 绘制箭头
        drawArrows(canvas);

        // 绘制轨迹点
        drawTrajectoryPoints(canvas);

        // 恢复画布状态
        canvas.restore();
    }

    /**
     * 绘制初始状态（无轨迹时）
     */
    private void drawInitialState(Canvas canvas) {
        // 可以绘制一个指南或提示
        pointPaint.setTextSize(40);
        pointPaint.setStyle(Paint.Style.FILL);
        canvas.drawText("开始测量以绘制轨迹", -180, 0, pointPaint);

        // 绘制一个虚线圆圈表示起点
        Paint circlePaint = new Paint(startPointPaint);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(3);
        circlePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        canvas.drawCircle(0, 0, 50, circlePaint);

        // 中心点
        canvas.drawCircle(0, 0, 15, startPointPaint);
    }

    /**
     * 绘制网格
     */
    private void drawGrid(Canvas canvas) {
        int gridCount = 20; // 网格线数量（每个方向）
        float gridPixelSize = GRID_SIZE * scale;

        // 绘制水平线
        for (int i = -gridCount; i <= gridCount; i++) {
            float y = i * gridPixelSize;
            canvas.drawLine(-gridCount * gridPixelSize, y, gridCount * gridPixelSize, y, gridPaint);
        }

        // 绘制垂直线
        for (int i = -gridCount; i <= gridCount; i++) {
            float x = i * gridPixelSize;
            canvas.drawLine(x, -gridCount * gridPixelSize, x, gridCount * gridPixelSize, gridPaint);
        }
    }

    /**
     * 绘制轨迹路径
     */
    private void drawTrajectoryPath(Canvas canvas) {
        if (trajectoryPoints == null || trajectoryPoints.size() < 2) {
            return;
        }

        trajectoryPath.reset();

        for (int i = 0; i < trajectoryPoints.size(); i++) {
            TrajectoryPoint point = trajectoryPoints.get(i);
            float x = (float) point.getX() * scale;
            float y = -(float) point.getY() * scale;  // Y轴向上为正，绘图坐标系Y轴向下为正，需要取反

            if (i == 0) {
                trajectoryPath.moveTo(x, y);
            } else {
                trajectoryPath.lineTo(x, y);
            }
        }

        // 检查是否需要闭合轨迹
        TrajectoryPoint firstPoint = trajectoryPoints.get(0);
        TrajectoryPoint lastPoint = trajectoryPoints.get(trajectoryPoints.size() - 1);
        double dx = lastPoint.getX() - firstPoint.getX();
        double dy = lastPoint.getY() - firstPoint.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 如果起点和终点距离小于阈值，则绘制一条从终点到起点的线
        if (distance < 2.0) { // 2米作为阈值
            float startX = (float) firstPoint.getX() * scale;
            float startY = -(float) firstPoint.getY() * scale;
            float endX = (float) lastPoint.getX() * scale;
            float endY = -(float) lastPoint.getY() * scale;

            // 使用虚线绘制闭合部分
            Paint closingPaint = new Paint(pathPaint);
            closingPaint.setStyle(Paint.Style.STROKE);
            closingPaint.setStrokeWidth(6);
            closingPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

            Path closingPath = new Path();
            closingPath.moveTo(endX, endY);
            closingPath.lineTo(startX, startY);

            canvas.drawPath(closingPath, closingPaint);
        }

        canvas.drawPath(trajectoryPath, pathPaint);
    }

    /**
     * 绘制轨迹点
     */
    private void drawTrajectoryPoints(Canvas canvas) {
        if (trajectoryPoints == null || trajectoryPoints.isEmpty()) {
            return;
        }

        for (int i = 0; i < trajectoryPoints.size(); i++) {
            TrajectoryPoint point = trajectoryPoints.get(i);
            float x = (float) point.getX() * scale;
            float y = -(float) point.getY() * scale;

            if (i == 0) {
                // 起点使用绿色，并且绘制大一点
                canvas.drawCircle(x, y, 12, startPointPaint);
            } else if (i == trajectoryPoints.size() - 1) {
                // 终点使用红色，并且绘制大一点
                canvas.drawCircle(x, y, 12, pointPaint);

                // 在终点绘制方向箭头
                if (trajectoryPoints.size() > 1) {
                    // 获取倒数第二个点，用于计算箭头方向
                    TrajectoryPoint prevPoint = trajectoryPoints.get(trajectoryPoints.size() - 2);
                    float prevX = (float) prevPoint.getX() * scale;
                    float prevY = -(float) prevPoint.getY() * scale;

                    // 计算方向角度
                    float dx = x - prevX;
                    float dy = y - prevY;
                    float angle = (float) Math.atan2(dy, dx);

                    // 绘制箭头
                    drawHeadArrow(canvas, x, y, angle);
                }
            } else if (i % 5 == 0) {
                // 每隔几个点绘制一个小点
                canvas.drawCircle(x, y, 6, pointPaint);
            }
        }
    }

    /**
     * 在轨迹终点绘制箭头
     */
    private void drawHeadArrow(Canvas canvas, float x, float y, float angle) {
        float arrowSize = 30f; // 箭头大小

        // 保存画布状态
        canvas.save();

        // 移动画布到箭头位置
        canvas.translate(x, y);

        // 旋转画布到箭头方向
        canvas.rotate((float) Math.toDegrees(angle));

        // 绘制箭头
        arrowPath.reset();
        arrowPath.moveTo(arrowSize, 0);
        arrowPath.lineTo(-arrowSize/2, arrowSize/2);
        arrowPath.lineTo(-arrowSize/3, 0);
        arrowPath.lineTo(-arrowSize/2, -arrowSize/2);
        arrowPath.close();

        canvas.drawPath(arrowPath, arrowPaint);

        // 恢复画布状态
        canvas.restore();
    }

    /**
     * 在指定位置绘制箭头
     */
    private void drawArrows(Canvas canvas) {
        if (trajectoryPoints == null || trajectoryPoints.size() < 2) {
            return;
        }

        // 不在轨迹上绘制箭头，改为只在终点绘制
        // 在drawTrajectoryPoints方法中已处理
    }

    /**
     * 计算合适的缩放比例和偏移量，使轨迹能完整显示在视图中
     */
    private void calculateScaleAndOffset() {
        if (trajectoryPoints == null || trajectoryPoints.isEmpty()) {
            return;
        }

        // 计算轨迹的边界
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (TrajectoryPoint point : trajectoryPoints) {
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }

        // 计算轨迹的宽高
        double width = maxX - minX;
        double height = maxY - minY;

        // 添加边距
        width += 2;  // 每边增加1米边距
        height += 2; // 每边增加1米边距

        // 计算中心点坐标
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        // 计算合适的缩放比例，确保轨迹能完整显示在视图中
        // 保留一些边距
        float marginFactor = 0.85f;  // 85%的视图区域用于显示轨迹

        if (width > 0 && height > 0) {
            float scaleX = (getWidth() * marginFactor) / (float)width;
            float scaleY = (getHeight() * marginFactor) / (float)height;
            scale = Math.min(scaleX, scaleY);

            // 确保缩放比例不会太小
            scale = Math.max(scale, initialScale / 2);
        } else {
            scale = initialScale;  // 默认缩放比例
        }

        // 计算偏移量，使轨迹中心与视图中心对齐
        offsetX = -(float)centerX * scale;
        offsetY = (float)centerY * scale;  // Y轴需要取反
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 视图大小变化时，重新计算缩放和偏移
        firstDraw = true;
    }
}