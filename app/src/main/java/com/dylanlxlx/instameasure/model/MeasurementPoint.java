package com.dylanlxlx.instameasure.model;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;

/**
 * AR测量中的3D测量点
 */
public class MeasurementPoint {
    private int id;              // 点ID
    private Anchor anchor;       // AR锚点
    private AnchorNode anchorNode; // AR场景节点
    private float[] position;    // 3D位置 [x,y,z]
    private Color color;         // 点颜色

    public MeasurementPoint(int id, Anchor anchor) {
        this.id = id;
        this.anchor = anchor;
        this.position = new float[3];

        // 默认颜色 - 首个点为绿色，其他为红色
        this.color = (id == 0) ? new Color(0f, 1f, 0f) : new Color(1f, 0f, 0f);
    }

    // 更新点的3D位置
    public void updatePosition(float[] position) {
        this.position = position;
    }

    // 从AnchorNode获取3D位置
    public void updatePositionFromAnchorNode() {
        if (anchorNode != null) {
            Vector3 worldPosition = anchorNode.getWorldPosition();
            position[0] = worldPosition.x;
            position[1] = worldPosition.y;
            position[2] = worldPosition.z;
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public float[] getPosition() {
        return position;
    }

    public AnchorNode getAnchorNode() {
        return anchorNode;
    }

    public void setAnchorNode(AnchorNode anchorNode) {
        this.anchorNode = anchorNode;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    // 清理资源
    public void detach() {
        if (anchorNode != null) {
            anchorNode.setParent(null);
            anchorNode = null;
        }
        if (anchor != null) {
            anchor.detach();
            anchor = null;
        }
    }
}