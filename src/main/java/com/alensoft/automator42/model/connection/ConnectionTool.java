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
public class ConnectionTool {
    private final Canvas canvas;
    private final ConnectionManager connectionManager;
    private Step sourceStep;
    private Group tempArrow;
    private ObjectProperty<Point2D> tempStartPoint;
    private ObjectProperty<Point2D> tempEndPoint;
    private ConnectionType connectionType = ConnectionType.DOWN;

    public ConnectionTool(Canvas canvas, ConnectionManager connectionManager) {
        this.canvas = canvas;
        this.connectionManager = connectionManager;
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
        connectionType = determineConnectionType(sourceStep, e.getX(), e.getY());

        // Получить начальную точку от узла
        tempStartPoint = new SimpleObjectProperty<>(connectionType.getSourcePoint(sourceStep).get());

        // Конечная точка следует за курсором
        tempEndPoint = new SimpleObjectProperty<>(new Point2D(e.getSceneX(), e.getSceneY()));

        // Создать временную стрелку того же типа, что будет создана
        tempArrow = new Arrow(tempStartPoint, tempEndPoint, connectionType.getDirection());

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
                connectionManager.createConnection(sourceStep, targetStep, connectionType);
            } catch (IllegalArgumentException ex) {
                System.err.println("Cannot create connection: " + ex.getMessage());
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
    private ConnectionType determineConnectionType(Step step, double xClick, double yClick) {
        double width = step.getWidth();
        double height = step.getHeight();

        // Правая треть - RIGHT/YES
        if (xClick > 2 * width / 3) {
            return ConnectionType.BRANCH;
        }
        // Центр - MAIN
        return ConnectionType.DOWN;
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
    public void setDefaultConnectionType(ConnectionType type) {
        this.connectionType = type;
    }
}