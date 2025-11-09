package com.alensoft.automator42.model.step;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.concurrent.atomic.AtomicInteger;

public class Null extends Step {
    private final Rectangle rect;
    public static final AtomicInteger counter = new AtomicInteger(0);
    private static long time = System.currentTimeMillis();
    public Null() {
        super("");
        if (System.currentTimeMillis()-time>500){
            counter.set(0);
        }
        time = System.currentTimeMillis();
        label.setText("       #"+counter.incrementAndGet());
        rect = new Rectangle(Step.WIDTH/2., Step.HEIGHT/3.);
        rect.setArcWidth(6);
        rect.setArcHeight(6);
        postConstruct(rect, Color.GRAY, Color.WHITE, 1, WIDTH/2, HEIGHT/3);
    }

    @Override
    protected void resize() {
        rect.setWidth(getPrefWidth());
        rect.setHeight(getPrefHeight());
        label.setPrefWidth(getPrefWidth());
        layoutLabelCentered();
    }
}
