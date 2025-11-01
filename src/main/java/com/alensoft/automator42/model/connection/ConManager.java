package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.line.Arrow;
import com.alensoft.automator42.model.step.Step;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.layout.Pane;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ConManager {
    private final Pane canvas;

    public ConManager(Pane canvas) {
        this.canvas = canvas;
    }

    public Connect createCon(Step source, Step target, ConType type) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target steps cannot be null");
        }

        if (source == target) {
            throw new IllegalArgumentException("Cannot connect step to itself");
        }
        Optional<Connect> exists = getCon(source, target, type);
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
        Connect con = new Connect(source, target, type, arrow);

        // Сохранить в карты
        source.out().add(con);
        target.in().add(con);
        return con;
    }

    /**
     * Удалить соединение
     */
    public void removeCon(Connect con) {
        if (con == null) return;

        Step source = con.getSource();
        Step target = con.getTarget();

        if (source != null) {
            source.out().remove(con);
        }

        if (target != null) {
            target.in().remove(con);
        }

        // Удалить с канваса
        canvas.getChildren().remove(con.getArrow());
    }

    /**
     * Удалить все соединения узла
     */
    public void removeAllCons(Step step) {
        if (step == null) return;
        List<Connect> prepareAll = Stream.of(step.in(), step.out())
                .flatMap(Collection::stream)
                .toList();
        //not merge! Concurrent modification exception
        prepareAll.forEach(this::removeCon);
    }


    /**
     * Получить конкретное соединение
     */
    public Optional<Connect> getCon(Step source, Step target, ConType type) {
        return source == null || target == null
                ? Optional.empty()
                : source.out().stream()
                .filter(c -> c.getTarget().equals(target) && c.getType() == type)
                .findAny();
    }

    /**
     * Получить соединение определенного типа от узла
     */
    public Optional<Connect> getConByType(Step source, ConType... types) {
        return source == null
                ? Optional.empty()
                : source.out().stream()
                .filter(c -> Arrays.asList(types).contains(c.getType()))
                .findFirst();

    }


}