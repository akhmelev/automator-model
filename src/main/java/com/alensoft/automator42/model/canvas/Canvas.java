package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.BaseNode;
import com.alensoft.automator42.model.connection.Connection;
import com.alensoft.automator42.model.connection.ConnectionManager;
import com.alensoft.automator42.model.connection.ConnectionType;
import com.alensoft.automator42.model.node.Begin;
import com.alensoft.automator42.model.node.Branch;
import com.alensoft.automator42.model.node.Connector;
import com.alensoft.automator42.model.node.End;
import javafx.scene.layout.Pane;

import java.util.*;

public class Canvas extends Pane {

    private BaseNode selectedNode;
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
        selectedNode = begin;
    }

    public BaseNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(BaseNode currentNode) {
        this.selectedNode = currentNode;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    // ============= ДОБАВЛЕНИЕ УЗЛОВ =============

    /**
     * Добавить узел в основную цепочку (MAIN flow)
     */
    public BaseNode addMainNode(final BaseNode prev, final BaseNode node) {
        return insertNode(prev, node, ConnectionType.DOWN);
    }


    public BaseNode addBranch(final BaseNode prev, final BaseNode branch) {
        if (!(branch instanceof Branch)) {
            throw new IllegalArgumentException("Node must be a Decision");
        }
        insertNode(prev, branch, ConnectionType.DOWN);
        Connection down = connectionManager.getConnectionByType(branch, ConnectionType.DOWN);
        BaseNode next = down.getTarget();
        connectionManager.createConnection(branch, next, ConnectionType.EMPTY);
        return branch;
    }

    public BaseNode insertToBranch(final BaseNode branch, final BaseNode node) {
        if (!(branch instanceof Branch)) {
            throw new IllegalArgumentException("Node must be a Decision");
        }
        Connection oldConnection = connectionManager.getConnectionByType(branch, ConnectionType.EMPTY);
        ConnectionType outType;
        if (oldConnection != null) {
            outType = ConnectionType.MERGE;
        } else {
            outType = ConnectionType.DOWN;
            oldConnection = connectionManager.getConnectionByType(branch, ConnectionType.BRANCH);
        }
        connectionManager.removeConnection(oldConnection);
        BaseNode next = oldConnection.getTarget();
        insertNode(branch, node, ConnectionType.BRANCH);
        connectionManager.createConnection(node, next, outType);
        return branch;
    }


    // ============= ВНУТРЕННЯЯ ЛОГИКА ВСТАВКИ =============

    /**
     * Универсальная вставка узла с сохранением AST
     */
    private BaseNode insertNode(final BaseNode prev, final BaseNode node, ConnectionType insertionType) {
        if (prev == null || node == null) {
            throw new IllegalArgumentException("Nodes cannot be null");
        }

        int fullStep = BaseNode.HEIGHT + BaseNode.STEP;

        // Добавить узел на canvas
        getChildren().add(node);

        double layoutX = prev.getLayoutX();
        double layoutY = prev.getLayoutY();
        node.relocate(layoutX, layoutY + fullStep);

        // Найти соединение от prev
        Connection prevConnection = connectionManager.getConnectionByType(prev, insertionType);

        if (prevConnection != null) {
            // Есть следующий узел - вставляемся между ними
            BaseNode nextNode = prevConnection.getTarget();

            // Сдвинуть все узлы вниз от точки вставки
            shiftNodesDown(nextNode, fullStep);

            // Переподключить: prev -> node -> next
            connectionManager.removeConnection(prevConnection);
            connectionManager.createConnection(prev, node, insertionType);
            connectionManager.createConnection(node, nextNode, ConnectionType.DOWN);
        } else {
            // Нет следующего узла - просто добавляем
            connectionManager.createConnection(prev, node, insertionType);
        }

        return node;
    }


    /**
     * Вставить узел между двумя существующими узлами
     */
    private BaseNode insertBetween(BaseNode source, BaseNode target, BaseNode newNode, ConnectionType type) {
        int fullStep = BaseNode.HEIGHT + BaseNode.STEP;

        // Добавить узел на canvas
        getChildren().add(newNode);

        // Позиционировать между source и target
        double x = source.getLayoutX();
        double y = source.getLayoutY() + fullStep;

        newNode.relocate(x, y);

        // Сдвинуть target и всё что после него
        shiftNodesDown(target, fullStep);

        // Найти и удалить старое соединение source -> target
        Connection oldConn = connectionManager.getConnection(source, target, type);
        ConnectionType oldType = oldConn != null ? oldConn.getType() : ConnectionType.DOWN;
        if (oldConn != null) {

            connectionManager.removeConnection(oldConn);
        }

        // Создать новые соединения: source -> newNode -> target
        connectionManager.createConnection(source, newNode, type);


        connectionManager.createConnection(newNode, target, oldType);

        return newNode;
    }

    /**
     * Вставка Decision внутрь ветки - создать его коннекторы к целевому узлу
     */
    private void insertDecisionIntoBranch(BaseNode decision, BaseNode target, double baseX, double baseY) {
        int fullStep = BaseNode.HEIGHT + BaseNode.STEP;
        shiftNodesDown(target, fullStep);
        connectionManager.createConnection(decision, target, ConnectionType.DOWN);
        connectionManager.createConnection(decision, target, ConnectionType.BRANCH);
    }

    /**
     * Сдвинуть узел и все зависимые от него узлы вниз
     * Использует BFS для обхода всех потомков
     */
    private void shiftNodesDown(BaseNode startNode, double offset) {
        if (startNode == null) return;

        Set<BaseNode> visited = new HashSet<>();
        Queue<BaseNode> queue = new LinkedList<>();

        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            BaseNode current = queue.poll();
            current.setLayoutY(current.getLayoutY() + offset);

            // Добавить всех потомков
            List<Connection> outgoing = connectionManager.getOutgoingConnections(current);
            for (Connection conn : outgoing) {
                BaseNode child = conn.getTarget();
                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.add(child);
                }
            }
        }
    }

    // ============= УДАЛЕНИЕ УЗЛОВ =============

    /**
     * Удалить узел с сохранением связей (reconnect соседей)
     */
    public void removeNode(BaseNode node) {
        if (node == null) return;

        // Нельзя удалить Begin
        if (node instanceof Begin) {
            throw new IllegalArgumentException("Cannot remove Begin node");
        }

        // Получить входящие и исходящие соединения
        List<Connection> incoming = connectionManager.getIncomingConnections(node);
        List<Connection> outgoing = connectionManager.getOutgoingConnections(node);

        // Если узел - Connector, просто удаляем его и переподключаем
        if (node instanceof Connector) {
            removeConnector(node, incoming, outgoing);
            return;
        }

        // Если узел - Decision, удалить его коннекторы
        if (node instanceof Branch) {
            removeDecisionWithConnectors(node, incoming, outgoing);
            return;
        }

        // Обычный узел - переподключить соседей
        if (!incoming.isEmpty() && !outgoing.isEmpty()) {
            reconnectNeighbors(incoming, outgoing);
        }

        // Сдвинуть узлы вверх после удаления
        if (!outgoing.isEmpty()) {
            int fullStep = BaseNode.HEIGHT + BaseNode.STEP;
            for (Connection conn : outgoing) {
                shiftNodesUp(conn.getTarget(), fullStep);
            }
        }

        // Удалить все соединения и сам узел
        connectionManager.removeAllConnections(node);
        getChildren().remove(node);

        // Обновить lastNode если удалили его
        if (selectedNode == node) {
            selectedNode = findNewLastNode();
        }
    }

    /**
     * Удалить Connector и переподключить его соседей
     */
    private void removeConnector(BaseNode connector, List<Connection> incoming, List<Connection> outgoing) {
        for (Connection in : incoming) {
            for (Connection out : outgoing) {
                try {
                    connectionManager.createConnection(in.getSource(), out.getTarget(), out.getType());
                } catch (IllegalArgumentException e) {
                    // Соединение уже существует
                }
            }
        }

        connectionManager.removeAllConnections(connector);
        getChildren().remove(connector);
    }

    /**
     * Удалить Decision вместе с его коннекторами
     */
    private void removeDecisionWithConnectors(BaseNode decision, List<Connection> incoming, List<Connection> outgoing) {
        // Найти коннекторы YES и NO
        Set<BaseNode> connectorsToRemove = new HashSet<>();
        Set<BaseNode> finalTargets = new HashSet<>();

        for (Connection out : outgoing) {
            BaseNode target = out.getTarget();
            if (target instanceof Connector) {
                connectorsToRemove.add(target);
                // Найти куда ведет коннектор
                List<Connection> connectorOutgoing = connectionManager.getOutgoingConnections(target);
                for (Connection conn : connectorOutgoing) {
                    finalTargets.add(conn.getTarget());
                }
            } else {
                finalTargets.add(target);
            }
        }

        // Переподключить входящие соединения к финальным целям
        for (Connection in : incoming) {
            for (BaseNode finalTarget : finalTargets) {
                try {
                    connectionManager.createConnection(in.getSource(), finalTarget, in.getType());
                } catch (IllegalArgumentException e) {
                    // Игнорируем дубликаты
                }
            }
        }

        // Удалить Decision и его коннекторы
        connectionManager.removeAllConnections(decision);
        getChildren().remove(decision);

        for (BaseNode connector : connectorsToRemove) {
            connectionManager.removeAllConnections(connector);
            getChildren().remove(connector);
        }

        // Сдвинуть финальные цели вверх
        int fullStep = BaseNode.HEIGHT + BaseNode.STEP;
        for (BaseNode target : finalTargets) {
            shiftNodesUp(target, fullStep * 2); // *2 для Decision + коннекторов
        }
    }

    /**
     * Переподключить соседей при удалении узла
     */
    private void reconnectNeighbors(List<Connection> incoming, List<Connection> outgoing) {
        for (Connection in : incoming) {
            BaseNode source = in.getSource();
            ConnectionType inType = in.getType();

            for (Connection out : outgoing) {
                BaseNode target = out.getTarget();
                ConnectionType outType = out.getType();

                // Сохранить тип соединения (приоритет у входящего типа)
                ConnectionType reconnectType = inType != ConnectionType.DOWN ? inType : outType;

                try {
                    connectionManager.createConnection(source, target, reconnectType);
                } catch (IllegalArgumentException e) {
                    // Соединение уже существует - игнорируем
                }
            }
        }
    }

    /**
     * Сдвинуть узел и зависимые узлы вверх
     */
    private void shiftNodesUp(BaseNode startNode, double offset) {
        if (startNode == null) return;

        Set<BaseNode> visited = new HashSet<>();
        Queue<BaseNode> queue = new LinkedList<>();

        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            BaseNode current = queue.poll();
            current.setLayoutY(current.getLayoutY() - offset);

            List<Connection> outgoing = connectionManager.getOutgoingConnections(current);
            for (Connection conn : outgoing) {
                BaseNode child = conn.getTarget();
                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.add(child);
                }
            }
        }
    }

    /**
     * Найти новый lastNode после удаления текущего
     */
    private BaseNode findNewLastNode() {
        return getChildren().stream()
                .filter(node -> node instanceof BaseNode)
                .map(node -> (BaseNode) node)
                .filter(node -> connectionManager.getConnectionByType(node, ConnectionType.DOWN) == null)
                .filter(node -> !(node instanceof Connector)) // Не коннекторы
                .findFirst()
                .orElse(null);
    }

    // ============= СЛУЖЕБНЫЕ МЕТОДЫ =============

    /**
     * Очистить все соединения
     */
    public void clearConnections() {
        connectionManager.clear();
    }

    /**
     * Проверить валидность AST (все узлы связаны, от Begin можно дойти до End)
     */
    public boolean validateAST() {
        BaseNode root = getRootNode();
        if (root == null) return false;

        Set<BaseNode> reachable = new HashSet<>();
        Queue<BaseNode> queue = new LinkedList<>();

        queue.add(root);
        reachable.add(root);

        while (!queue.isEmpty()) {
            BaseNode current = queue.poll();
            List<Connection> outgoing = connectionManager.getOutgoingConnections(current);

            for (Connection conn : outgoing) {
                BaseNode target = conn.getTarget();
                if (!reachable.contains(target)) {
                    reachable.add(target);
                    queue.add(target);
                }
            }
        }

        // Все узлы должны быть достижимы из Begin
        long totalNodes = getChildren().stream()
                .filter(node -> node instanceof BaseNode)
                .count();

        return reachable.size() == totalNodes;
    }

    /**
     * Получить корневой узел (Begin)
     */
    public BaseNode getRootNode() {
        return getChildren().stream()
                .filter(node -> node instanceof Begin)
                .map(node -> (BaseNode) node)
                .findFirst()
                .orElse(null);
    }
}