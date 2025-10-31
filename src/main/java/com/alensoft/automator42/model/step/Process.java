package com.alensoft.automator42.model.step;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Process extends Step {
    private final Rectangle rect;

    public Process(String text) {
        super(text);
        rect = new Rectangle(Step.WIDTH, Step.HEIGHT);
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
