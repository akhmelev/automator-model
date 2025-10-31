package com.alensoft.automator42.model.step;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Connector extends Step {
    private final Circle circle;

    public Connector() {
        super("");
        circle = new Circle(8);
        postConstruct(circle, Color.DARKSLATEBLUE, Color.web("#e8ecff"), 1.5, 18, 18);
    }

    @Override
    protected void resize() {
        double w = getPrefWidth();
        double h = getPrefHeight();
        circle.setRadius(Math.max(6, Math.min(w, h) / 2 - 2));
        circle.setCenterX(w / 2);
        circle.setCenterY(h / 2);
    }
}
