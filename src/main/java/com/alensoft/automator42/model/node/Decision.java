package com.alensoft.automator42.model.node;

import com.alensoft.automator42.model.BaseNode;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;

/**
 * Узел принятия решения с метками Yes/No
 */
public class Decision extends BaseNode {
    private final Polygon diamond = new Polygon();
    private final Label yesLabel = new Label("Yes");
    private final Label noLabel = new Label("No");

    public Decision(String text) {
        super(text);
        
        // Настройка меток
        yesLabel.setFont(Font.font(10));
        yesLabel.setTextFill(Color.DARKORANGE);
        noLabel.setFont(Font.font(10));
        noLabel.setTextFill(Color.DARKORANGE);
        
        getChildren().addAll(yesLabel, noLabel);
        
        postConstruct(diamond, Color.DARKORANGE, Color.web("#fff7e6"), 2, WIDTH, HEIGHT);
    }

    @Override
    protected void resize() {
        double w = getPrefWidth();
        double h = getPrefHeight();
        
        // Форма ромба (шестиугольник для ДРАКОН)
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
        yesLabel.setLayoutX(w - 30);
        yesLabel.setLayoutY(h / 2 - 10);
        
        noLabel.setLayoutX(5);
        noLabel.setLayoutY(h / 2 - 10);
    }
}