package com.alensoft.automator42.model.node;

import com.alensoft.automator42.model.BaseNode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ProcessNode extends BaseNode {
    private final Rectangle rect;

    public ProcessNode(String text) {
        super(text);
        rect = new Rectangle(BaseNode.WIDTH, BaseNode.HEIGHT);
        rect.setArcWidth(6);
        rect.setArcHeight(6);
        postConstruct(rect, Color.DARKGREEN, Color.web("#ffffff"), 2, WIDTH, HEIGHT);
    }

    @Override
    protected void resize() {
        rect.setWidth(getPrefWidth());
        rect.setHeight(getPrefHeight());
        label.setPrefWidth(getPrefWidth());
        layoutLabelCentered();
    }
}
