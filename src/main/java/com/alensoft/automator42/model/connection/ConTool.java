package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.step.Step;
import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.line.Arrow;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Инструмент для интерактивного создания связей между узлами.
 * Держим Ctrl и тянем от одного узла к другому.
 */
public class ConTool {
    private final Canvas canvas;
    private final ConManager conManager;
    private Step sourceStep;
    private Group tempArrow;
    private ObjectProperty<Point2D> tempStartPoint;
    private ObjectProperty<Point2D> tempEndPoint;
    private ConType conType = ConType.OK;

    public ConTool(Canvas canvas, ConManager conManager) {
        this.canvas = canvas;
        this.conManager = conManager;
    }

    /**
     * Активировать инструмент на узле
     */
    public void activate(Step step) {
        // Добавляем обработчики через addEventFilter чтобы не блокировать drag
        step.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onStepPressed);
        step.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onStepDragged);
        step.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onStepReleased);
    }

    private void onStepPressed(MouseEvent e) {
        // Только если зажат Ctrl
        if (e.getButton() != MouseButton.PRIMARY || !e.isControlDown()) {
            return; // Не consume, пусть обработает drag
        }

        e.consume(); // Теперь consume, чтобы не перетаскивать
        sourceStep = (Step) e.getSource();
        canvas.setSelectedStep(sourceStep);
        // Определить тип соединения по позиции клика
        conType = determineConType(sourceStep, e.getX(), e.getY());

        // Получить начальную точку от узла
        tempStartPoint = new SimpleObjectProperty<>(conType.getSourcePoint(sourceStep).get());

        // Конечная точка следует за курсором
        tempEndPoint = new SimpleObjectProperty<>(new Point2D(e.getSceneX(), e.getSceneY()));

        // Создать временную стрелку того же типа, что будет создана
        tempArrow = new Arrow(tempStartPoint, tempEndPoint, conType.getDirection());

        tempArrow.setOpacity(0.5); // Полупрозрачная для визуального отличия
        tempArrow.setMouseTransparent(true);

        canvas.getChildren().add(tempArrow);
    }

    private void onStepDragged(MouseEvent e) {
        if (tempArrow == null || !e.isControlDown()) {
            return; // Не consume, пусть обработает drag
        }

        e.consume();

        // Обновляем конечную точку - стрелка автоматически перерисуется благодаря binding
        tempEndPoint.set(new Point2D(e.getSceneX(), e.getSceneY()));
    }

    private void onStepReleased(MouseEvent e) {
        if (tempArrow == null) {
            return;
        }

        e.consume();
        canvas.getChildren().remove(tempArrow);

        // Найти целевой узел
        Step targetStep = findStepAt(e.getSceneX(), e.getSceneY());

        if (targetStep != null && targetStep != sourceStep) {
            try {
                conManager.createCon(sourceStep, targetStep, conType);
            } catch (IllegalArgumentException ex) {
                System.err.println("Cannot create con: " + ex.getMessage());
            }
        }

        sourceStep = null;
        tempArrow = null;
        tempStartPoint = null;
        tempEndPoint = null;
    }

    /**
     * Определить тип соединения по позиции клика на узле
     */
    private ConType determineConType(Step step, double xClick, double yClick) {
        double width = step.getWidth();
        double height = step.getHeight();

        // Правая треть - RIGHT/YES
        if (xClick > 2 * width / 3) {
            return ConType.IN;
        }
        // Центр - MAIN
        return ConType.OK;
    }

    /**
     * Найти узел в указанных координатах
     */
    private Step findStepAt(double sceneX, double sceneY) {
        return canvas.getChildren().stream()
                .filter(step -> step instanceof Step)
                .map(step -> (Step) step)
                .filter(step -> step.getBoundsInParent().contains(sceneX, sceneY))
                .findFirst()
                .orElse(null);
    }

    /**
     * Установить тип соединения по умолчанию
     */
    public void setDefaultConType(ConType type) {
        this.conType = type;
    }
}