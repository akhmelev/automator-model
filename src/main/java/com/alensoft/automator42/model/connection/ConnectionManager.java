package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.line.Arrow;
import com.alensoft.automator42.model.step.Step;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.layout.Pane;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class ConnectionManager {
    private final Pane canvas;

    public ConnectionManager(Pane canvas) {
        this.canvas = canvas;
    }

    public Connection createConnection(Step source, Step target, ConnectionType type) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target steps cannot be null");
        }

        if (source == target) {
            throw new IllegalArgumentException("Cannot connect step to itself");
        }
        Optional<Connection> exists = getConnection(source, target, type);
        // Проверка на дублирование
        if (exists.isPresent()) {
            return exists.get();
        }

        // Получить точки подключения
        ObjectProperty<Point2D> startPoint = type.getSourcePoint(source);
        ObjectProperty<Point2D> endPoint = type.getTargetPoint(target);

        // Создать стрелку
        Group arrow = new Arrow(startPoint, endPoint, type.getDirection());
        canvas.getChildren().add(arrow);
        arrow.toBack(); // Стрелки за узлами

        // Создать соединение
        Connection connection = new Connection(source, target, type, arrow);

        // Сохранить в карты
        source.out().add(connection);
        target.in().add(connection);
        return connection;
    }

    /**
     * Удалить соединение
     */
    public void removeConnection(Connection connection) {
        if (connection == null) return;

        Step source = connection.getSource();
        Step target = connection.getTarget();

        if (source != null) {
            source.out().remove(connection);
        }

        if (target != null) {
            target.in().remove(connection);
        }

        // Удалить с канваса
        canvas.getChildren().remove(connection.getArrow());
    }

    /**
     * Удалить все соединения узла
     */
    public void removeAllConnections(Step step) {
        if (step == null) return;
        Stream.of(step.in(), step.out())
                .flatMap(Collection::stream)
                .map(Connection::getArrow)
                .forEach(canvas.getChildren()::remove);
        step.in().clear();
        step.out().clear();
    }


    /**
     * Получить конкретное соединение
     */
    public Optional<Connection> getConnection(Step source, Step target, ConnectionType type) {
        return source == null || target == null
                ? Optional.empty()
                : source.out().stream()
                .filter(c -> c.getTarget().equals(target) && c.getType() == type)
                .findAny();
    }

    /**
     * Получить соединение определенного типа от узла
     */
    public Optional<Connection> getConnectionByType(Step source, ConnectionType type) {
        return source == null
                ? Optional.empty()
                : source.out().stream()
                .filter(c -> c.getType() == type)
                .findFirst();

    }


}