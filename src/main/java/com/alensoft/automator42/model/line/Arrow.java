package com.alensoft.automator42.model.line;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;


public class Arrow extends Group {


    private final Polyline polyline = new Polyline();
    private final Polygon head = new Polygon();
    private static final double V_SHORTEN = 3;
    public static final double H_SHORTEN = 5;
    public static final double H_OFFSET = 12.0;

    private final ObjectProperty<Point2D> startProperty;
    private final ObjectProperty<Point2D> endProperty;
    private final ArrowType direction;

    public Arrow(ObjectProperty<Point2D> start, ObjectProperty<Point2D> end, ArrowType direction) {
        this.startProperty = start;
        this.endProperty = end;
        this.direction = direction;
        polyline.setStrokeWidth(2);
        polyline.setStroke(Color.web("#2b2b2b"));

        head.getPoints().addAll(0.0, 0.0, -8.0, -4.0, -8.0, 4.0);
        head.setFill(Color.web("#2b2b2b"));
        start.addListener((obs, oldV, newV) -> updateArrow());
        end.addListener((obs, oldV, newV) -> updateArrow());
        //add text OK at start
        Label conLabel = new Label(direction.name());
        conLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2b2b2b;");
        conLabel.textProperty().bind(Bindings.createStringBinding(direction::name));
        conLabel.layoutXProperty().bind(Bindings.createDoubleBinding(() -> (start.getValue().getX()), startProperty, endProperty));
        conLabel.layoutYProperty().bind(Bindings.createDoubleBinding(() -> (start.getValue().getY()), startProperty, endProperty));

        getChildren().add(conLabel);

        getChildren().addAll(polyline, head);
        updateArrow();
    }

    private void updateArrow() {
        Point2D start = startProperty.get();
        Point2D end = endProperty.get();

        if (start == null || end == null) {
            polyline.getPoints().clear();
            head.setVisible(false);
            return;
        }

        head.setVisible(true);

        List<Point2D> points = calculatePath(start, end, direction);
        updateHead(points);

        // Переводим точки в список Double для Polyline
        List<Double> polylinePoints = new ArrayList<>();
        points.forEach(p -> {
            polylinePoints.add(p.getX());
            polylinePoints.add(p.getY());
        });
        // Обновление наконечника
        shortenLine(points, direction);
        polyline.getPoints().setAll(polylinePoints);
    }

    private List<Point2D> calculatePath(Point2D start, Point2D end, ArrowType direction) {
        List<Point2D> path = new ArrayList<>();
        path.add(start);

        double sx = start.getX();
        double sy = start.getY();
        double ex = end.getX();
        double ey = end.getY();

        double x = sx;
        double y = sy;
        direction = direction.update(start, end);
        for (int i = 0; i < direction.dXY.length; i++) {
            double delta = direction.dXY[i];
            x = Math.abs(delta) <= 1.0001
                    ? x + (ex - x) * delta
                    : x + delta;
            //это последняя точка и есть изменение по X
            if (i == direction.dXY.length - 2 && direction.dXY[i] != 0) {
                x = (sx > ex) ? x + H_SHORTEN : x - H_SHORTEN;
            }

            i++;
            delta = direction.dXY[i];
            y = Math.abs(delta) <= 1.0001
                    ? y + (ey - y) * delta
                    : y + delta;
            //это предпоследняя точка и нет изменения по Y на последней точке
            if (i == direction.dXY.length - 3 && direction.dXY[i + 1] == 1 && direction.dXY[i + 2] == 0) {
                y = (sx > ex) ? y - H_OFFSET : y + H_OFFSET;
            }
            //это последняя точка и есть изменение по Y
            if (i == direction.dXY.length - 1 && direction.dXY[i] != 0) {
                y = (sy > ey) ? y + V_SHORTEN : y - V_SHORTEN;
            }

            path.add(new Point2D(x, y));
        }
        return path;
    }

    private void shortenLine(List<Point2D> path, ArrowType direction) {
        Point2D last = path.get(path.size() - 1);
        if (direction == ArrowType.IN || direction == ArrowType.OK) {
            last = new Point2D(last.getX(), last.getY() - 4);
        } else {
            last = new Point2D(last.getX() + 4, last.getY());
        }
        path.set(path.size() - 1, last);
    }


    private void updateHead(List<Point2D> path) {
        Point2D last = path.get(path.size() - 1);
        Point2D prev = path.get(path.size() - 2);
        double sx = prev.getX();
        double sy = prev.getY();
        double ex = last.getX();
        double ey = last.getY();
        double angle = Math.atan2(ey - sy, ex - sx);

        double offset = 0; // Изменено на 0, так как линия уже укорочена

        double hx = ex - Math.cos(angle) * offset;
        double hy = ey - Math.sin(angle) * offset;

        head.setLayoutX(hx + 4);
        head.setLayoutY(hy - 1);

        head.setRotate(Math.toDegrees(angle));
    }

}