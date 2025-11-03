package com.alensoft.automator42.model.step;

import com.alensoft.automator42.model.connection.ConType;
import com.alensoft.automator42.model.connection.Connect;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class Step extends Pane {
    public static final int WIDTH = 120;
    public static final int HEIGHT = 40;
    public static final int STEP = 0;

    protected Label label = new Label();

    // Точки подключения для стрелок
    private final ObjectProperty<Point2D> top = new SimpleObjectProperty<>();
    private final ObjectProperty<Point2D> left = new SimpleObjectProperty<>();
    private final ObjectProperty<Point2D> right = new SimpleObjectProperty<>();
    private final ObjectProperty<Point2D> bottom = new SimpleObjectProperty<>();

    private final List<Connect> in = new ArrayList<>();
    private final List<Connect> out = new ArrayList<>();

    public Step(String text) {
        label.setText(text);
        label.setFont(Font.font(14));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        getChildren().add(label);

        enableDrag();
        bindAnchorPoints();
    }

    private void bindAnchorPoints() {
        top.bind(getBinding(this::getTopAnchor));
        left.bind(getBinding(this::getLeftAnchor));
        right.bind(getBinding(this::getRightAnchor));
        bottom.bind(getBinding(this::getBottomAnchor));
    }

    public void updateAnchors() {
        top.set(getTopAnchor());
        left.set(getLeftAnchor());
        right.set(getRightAnchor());
        bottom.set(getBottomAnchor());
    }

    private Point2D getTopAnchor() {
        Bounds b = localToParent(getBoundsInLocal());
        return new Point2D(b.getMinX() + b.getWidth() / 2.0, b.getMinY());
    }

    private Point2D getLeftAnchor() {
        Bounds b = localToParent(getBoundsInLocal());
        return new Point2D(b.getMinX(), b.getMinY() + b.getHeight() / 2.0);
    }

    private Point2D getRightAnchor() {
        Bounds b = localToParent(getBoundsInLocal());
        return new Point2D(b.getMinX() + b.getWidth(), b.getMinY() + b.getHeight() / 2.0);
    }

    private Point2D getBottomAnchor() {
        Bounds b = localToParent(getBoundsInLocal());
        return new Point2D(b.getMinX() + b.getWidth() / 2.0, b.getMinY() + b.getHeight());
    }

    private ObjectBinding<Point2D> getBinding(Callable<Point2D> func) {
        return Bindings.createObjectBinding(func,
                layoutBoundsProperty(), translateXProperty(),
                translateYProperty(), localToParentTransformProperty(),
                layoutXProperty(), layoutYProperty());
    }

    protected void postConstruct(Shape shape, Color strokeColor, Color fillColor,
                                 double strokeWidth, int prefWidth, int prefHeight) {
        shape.setStroke(strokeColor);
        shape.setFill(fillColor);
        shape.setStrokeWidth(strokeWidth);
        getChildren().add(0, shape);

        setPrefSize(prefWidth, prefHeight);
        widthProperty().addListener((o, a, b) -> resize());
        heightProperty().addListener((o, a, b) -> resize());
        resize();
    }

    protected abstract void resize();

    public ObjectProperty<Point2D> getTop() {
        return top;
    }

    public ObjectProperty<Point2D> getBottom() {
        return bottom;
    }

    public ObjectProperty<Point2D> getLeft() {
        return left;
    }

    public ObjectProperty<Point2D> getRight() {
        return right;
    }

    public List<Connect> in() {
        return in;
    }

    public List<Connect> out() {
        return out;
    }

    public String getText() {
        return label.getText();
    }

    public void setText(String text) {
        label.setText(text);
    }

    private void enableDrag() {
        final Delta dragDelta = new Delta();
        setOnMousePressed((MouseEvent e) -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dragDelta.x = e.getX();
            dragDelta.y = e.getY();
            toFront();
        });
        setOnMouseDragged((MouseEvent e) -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            double nx = getLayoutX() + e.getX() - dragDelta.x;
            double ny = getLayoutY() + e.getY() - dragDelta.y;
            relocate(nx, ny);
        });
    }

    protected void layoutLabelCentered() {
        double stepW = getPrefWidth();
        double stepH = getPrefHeight();

        double labelW = label.getPrefWidth();
        if (labelW <= 0) {
            labelW = stepW;
        }
        double labelH = label.prefHeight(labelW);

        label.setLayoutX((stepW - labelW) / 2);
        label.setLayoutY((stepH - labelH) / 2);
    }

    public List<Step> getNextSteps() {
        return out.stream()
                .filter(c-> c.getType() != ConType.EMPTY)
                .sorted()
                .distinct()
                .map(Connect::getTarget)
                .toList();
    }

    public List<Step> getPreviousSteps() {
        return in.stream()
                .filter(c-> c.getType() != ConType.EMPTY)
                .sorted()
                .distinct()
                .map(Connect::getSource)
                .toList();
    }

    private static class Delta {
        double x, y;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+" (" + (int)this.getLayoutX() + ", " + (int)this.getLayoutY() + ") "+label.getText();
    }
}