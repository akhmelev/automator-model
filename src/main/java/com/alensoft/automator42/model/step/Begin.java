package com.alensoft.automator42.model.step;

import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class Begin extends Step {
    private final Ellipse ellipse;

    public Begin(String text) {
        super(text);
        ellipse = new Ellipse(Step.WIDTH, Step.HEIGHT);
        postConstruct(ellipse, Color.DARKBLUE, Color.web("#fffafa"), 2, WIDTH, HEIGHT);
    }


    protected void resize() {
        double w = getPrefWidth();
        double h = getPrefHeight();
        ellipse.setRadiusX(Math.max(30, w / 2));
        ellipse.setRadiusY(Math.max(16, h / 2));
        ellipse.setCenterX(w / 2);
        ellipse.setCenterY(h / 2);
        label.setPrefWidth(w);
        layoutLabelCentered();
    }
}
