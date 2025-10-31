package com.alensoft.automator42.model.step;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;

/**
 * Узел принятия решения с метками Yes/No
 */
public class Branch extends Step {
    private final Polygon diamond = new Polygon();
    private final Label yesLabel = new Label("Yes");
    private final Label noLabel = new Label("No");

    public Branch(String text) {
        super(text);
        
        // Настройка меток
        yesLabel.setFont(Font.font(12));
        yesLabel.setTextFill(Color.BLUE);
        noLabel.setFont(Font.font(12));
        noLabel.setTextFill(Color.DARKRED);
        
        getChildren().addAll(yesLabel, noLabel);
        
        postConstruct(diamond, Color.DARKORANGE, Color.web("#fff7e6"), 2, WIDTH, HEIGHT);
    }

    @Override
    protected void resize() {
        double w = getPrefWidth();
        double h = getPrefHeight();
        
        diamond.getPoints().setAll(
                3 * w / 4, 0.0,      // Верхняя правая
                w, h / 2,            // Правая
                3 * w / 4, h,        // Нижняя правая
                w / 4, h,            // Нижняя левая
                0.0, h / 2,          // Левая
                w / 4, 0.0           // Верхняя левая
        );
        
        label.setPrefWidth(w - 20);
        layoutLabelCentered();
        
        // Позиционирование меток Yes/No
        yesLabel.setLayoutX(w /2 - 10);
        yesLabel.setLayoutY(h - 14);
        
        noLabel.setLayoutX( w-18);
        noLabel.setLayoutY(h / 2 - 10);
    }
}