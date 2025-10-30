package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.BaseNode;
import com.alensoft.automator42.model.connection.Connection;
import com.alensoft.automator42.model.connection.ConnectionManager;
import com.alensoft.automator42.model.connection.ConnectionType;
import com.alensoft.automator42.model.node.Begin;
import com.alensoft.automator42.model.node.End;
import javafx.scene.layout.Pane;

public class Canvas extends Pane {

    private BaseNode lastNode;
    private final ConnectionManager connectionManager;

    public Canvas(int x, int y) {
        this.setPrefSize(1000, 700);
        this.setStyle("-fx-background-color: linear-gradient(#f8f8f8, #e8eef8);");

        connectionManager = new ConnectionManager(this);

        Begin begin = new Begin("Start");
        begin.relocate(x, y);
        getChildren().add(begin);

        End end = new End("End");
        addMainNode(begin, end);
        lastNode = begin;
    }

    public BaseNode getLastNode() {
        return lastNode;
    }

    public void setLastNode(BaseNode currentNode) {
        this.lastNode =currentNode;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public BaseNode addMainNode(final BaseNode prev, final BaseNode node) {
        return addNode(prev, node, ConnectionType.MAIN);
    }

    public BaseNode addDecisionNode(final BaseNode prev, final BaseNode node) {
        return addNode(prev, node, ConnectionType.YES, ConnectionType.NO);
    }

    private BaseNode addNode(final BaseNode prev, final BaseNode node, ConnectionType... types) {
        int fullStep = BaseNode.HEIGHT + BaseNode.STEP;

        getChildren().add(node);
        double layoutX = prev.getLayoutX();
        double layoutY = prev.getLayoutY();
        node.relocate(layoutX, layoutY + fullStep);

        // Сдвинуть все узлы ниже
        Connection prevConnection = connectionManager.getConnectionByType(prev, ConnectionType.MAIN);
        if (prevConnection != null) {
            BaseNode current = prevConnection.getTarget();
            while (current != null) {
                current.setLayoutY(current.getLayoutY() + fullStep);
                Connection nextConn = connectionManager.getConnectionByType(current, ConnectionType.MAIN);
                current = nextConn != null ? nextConn.getTarget() : null;
            }
        }

        // Вставить новый узел
        if (prevConnection != null) {
            BaseNode oldNext = prevConnection.getTarget();
            connectionManager.removeConnection(prevConnection);
            connectionManager.createConnection(prev, node, ConnectionType.MAIN);
            for (ConnectionType connectionType : types) {
                connectionManager.createConnection(node, oldNext, connectionType);
            }
        } else {
            connectionManager.createConnection(prev, node, ConnectionType.MAIN);
        }
        return node;
    }

    public void removeNode(BaseNode node) {
        connectionManager.removeAllConnections(node);
        getChildren().remove(node);
    }

    public void clearConnections() {
        connectionManager.clear();
    }
}