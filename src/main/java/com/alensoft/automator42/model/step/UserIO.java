package com.alensoft.automator42.model.step;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class UserIO extends Step {
    private final Polygon para = new Polygon();

    public UserIO(String text) {
        super(text);
        postConstruct(para, Color.DARKMAGENTA, Color.web("#f7f0ff"), 2, WIDTH, HEIGHT);
    }

    @Override
    protected void resize() {
        double w = getPrefWidth();
        double h = getPrefHeight();
        double slant = Math.min(24, h / 3);
        para.getPoints().setAll(
                slant, 0.0,
                w - 0.0, 0.0,
                w - slant, h - 0.0,
                0.0, h - 0.0
        );
        label.setPrefWidth(w - 20);
        layoutLabelCentered();
    }
}
