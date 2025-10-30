package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.BaseNode;
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
    private BaseNode sourceNode;
    private Group tempArrow;
    private ObjectProperty<Point2D> tempStartPoint;
    private ObjectProperty<Point2D> tempEndPoint;
    private ConnectionType connectionType = ConnectionType.MAIN;

    public ConnectionTool(Canvas canvas, ConnectionManager connectionManager) {
        this.canvas = canvas;
        this.connectionManager = connectionManager;
    }

    /**
     * Активировать инструмент на узле
     */
    public void activate(BaseNode node) {
        // Добавляем обработчики через addEventFilter чтобы не блокировать drag
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onNodePressed);
        node.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onNodeDragged);
        node.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onNodeReleased);
    }

    private void onNodePressed(MouseEvent e) {
        // Только если зажат Ctrl
        if (e.getButton() != MouseButton.PRIMARY || !e.isControlDown()) {
            return; // Не consume, пусть обработает drag
        }

        e.consume(); // Теперь consume, чтобы не перетаскивать
        sourceNode = (BaseNode) e.getSource();
        canvas.setLastNode(sourceNode);
        // Определить тип соединения по позиции клика
        connectionType = determineConnectionType(sourceNode, e.getX(), e.getY());

        // Получить начальную точку от узла
        tempStartPoint = new SimpleObjectProperty<>(connectionType.getSourcePoint(sourceNode).get());

        // Конечная точка следует за курсором
        tempEndPoint = new SimpleObjectProperty<>(new Point2D(e.getSceneX(), e.getSceneY()));

        // Создать временную стрелку того же типа, что будет создана
        tempArrow = new Arrow(tempStartPoint, tempEndPoint, connectionType.getDirection());

        tempArrow.setOpacity(0.5); // Полупрозрачная для визуального отличия
        tempArrow.setMouseTransparent(true);

        canvas.getChildren().add(tempArrow);
    }

    private void onNodeDragged(MouseEvent e) {
        if (tempArrow == null || !e.isControlDown()) {
            return; // Не consume, пусть обработает drag
        }

        e.consume();

        // Обновляем конечную точку - стрелка автоматически перерисуется благодаря binding
        tempEndPoint.set(new Point2D(e.getSceneX(), e.getSceneY()));
    }

    private void onNodeReleased(MouseEvent e) {
        if (tempArrow == null) {
            return;
        }

        e.consume();
        canvas.getChildren().remove(tempArrow);

        // Найти целевой узел
        BaseNode targetNode = findNodeAt(e.getSceneX(), e.getSceneY());

        if (targetNode != null && targetNode != sourceNode) {
            try {
                connectionManager.createConnection(sourceNode, targetNode, connectionType);
            } catch (IllegalArgumentException ex) {
                System.err.println("Cannot create connection: " + ex.getMessage());
            }
        }

        sourceNode = null;
        tempArrow = null;
        tempStartPoint = null;
        tempEndPoint = null;
    }

    /**
     * Определить тип соединения по позиции клика на узле
     */
    private ConnectionType determineConnectionType(BaseNode node, double xClick, double yClick) {
        double width = node.getWidth();
        double height = node.getHeight();

        // Левая треть - LEFT/NO
        if (xClick < width / 3) {
            return ConnectionType.YES;
        }
        // Правая треть - RIGHT/YES
        if (xClick > 2 * width / 3) {
            return ConnectionType.NO;
        }
        // Центр - MAIN
        return ConnectionType.MAIN;
    }

    /**
     * Найти узел в указанных координатах
     */
    private BaseNode findNodeAt(double sceneX, double sceneY) {
        return canvas.getChildren().stream()
                .filter(node -> node instanceof BaseNode)
                .map(node -> (BaseNode) node)
                .filter(node -> node.getBoundsInParent().contains(sceneX, sceneY))
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