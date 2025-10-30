package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.BaseNode;
import com.alensoft.automator42.model.line.Arrow;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.layout.Pane;

import java.util.*;

/**
 * Менеджер соединений между узлами.
 * Централизованное управление всеми связями (SRP, DRY).
 */
public class ConnectionManager {
    private final Map<BaseNode, List<Connection>> outgoingConnections = new HashMap<>();
    private final Map<BaseNode, List<Connection>> incomingConnections = new HashMap<>();
    private final Pane canvas;

    public ConnectionManager(Pane canvas) {
        this.canvas = canvas;
    }

    /**
     * Создать соединение между узлами
     */
    public Connection createConnection(BaseNode source, BaseNode target, ConnectionType type) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target nodes cannot be null");
        }

        if (source == target) {
            throw new IllegalArgumentException("Cannot connect node to itself");
        }

        // Проверка на дублирование
        if (hasConnection(source, target, type)) {
            return getConnection(source, target, type);
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
        outgoingConnections.computeIfAbsent(source, k -> new ArrayList<>()).add(connection);
        incomingConnections.computeIfAbsent(target, k -> new ArrayList<>()).add(connection);

        return connection;
    }

    /**
     * Удалить соединение
     */
    public void removeConnection(Connection connection) {
        if (connection == null) return;

        BaseNode source = connection.getSource();
        BaseNode target = connection.getTarget();

        // Удалить из карт
        List<Connection> outgoing = outgoingConnections.get(source);
        if (outgoing != null) {
            outgoing.remove(connection);
            if (outgoing.isEmpty()) {
                outgoingConnections.remove(source);
            }
        }

        List<Connection> incoming = incomingConnections.get(target);
        if (incoming != null) {
            incoming.remove(connection);
            if (incoming.isEmpty()) {
                incomingConnections.remove(target);
            }
        }

        // Удалить стрелку с канваса
        canvas.getChildren().remove(connection.getArrow());
    }

    /**
     * Удалить все соединения узла
     */
    public void removeAllConnections(BaseNode node) {
        if (node == null) return;

        // Удалить исходящие
        List<Connection> outgoing = outgoingConnections.remove(node);
        if (outgoing != null) {
            for (Connection conn : new ArrayList<>(outgoing)) {
                removeConnection(conn);
            }
        }

        // Удалить входящие
        List<Connection> incoming = incomingConnections.remove(node);
        if (incoming != null) {
            for (Connection conn : new ArrayList<>(incoming)) {
                removeConnection(conn);
            }
        }
    }

    /**
     * Получить все исходящие соединения узла
     */
    public List<Connection> getOutgoingConnections(BaseNode node) {
        if (node == null) return Collections.emptyList();
        return Collections.unmodifiableList(
                outgoingConnections.getOrDefault(node, Collections.emptyList())
        );
    }

    /**
     * Получить все входящие соединения узла
     */
    public List<Connection> getIncomingConnections(BaseNode node) {
        if (node == null) return Collections.emptyList();
        return Collections.unmodifiableList(
                incomingConnections.getOrDefault(node, Collections.emptyList())
        );
    }

    /**
     * Получить все соединения узла (входящие + исходящие)
     */
    public List<Connection> getAllConnections(BaseNode node) {
        if (node == null) return Collections.emptyList();

        List<Connection> all = new ArrayList<>();
        all.addAll(getOutgoingConnections(node));
        all.addAll(getIncomingConnections(node));
        return all;
    }

    /**
     * Проверить наличие соединения
     */
    public boolean hasConnection(BaseNode source, BaseNode target, ConnectionType type) {
        if (source == null || target == null) return false;

        List<Connection> connections = outgoingConnections.get(source);
        if (connections == null) return false;

        return connections.stream()
                .anyMatch(c -> c.getTarget().equals(target) && c.getType() == type);
    }

    /**
     * Получить конкретное соединение
     */
    public Connection getConnection(BaseNode source, BaseNode target, ConnectionType type) {
        if (source == null || target == null) return null;

        List<Connection> connections = outgoingConnections.get(source);
        if (connections == null) return null;

        return connections.stream()
                .filter(c -> c.getTarget().equals(target) && c.getType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Получить соединение определенного типа от узла
     */
    public Connection getConnectionByType(BaseNode source, ConnectionType type) {
        if (source == null) return null;

        List<Connection> connections = outgoingConnections.get(source);
        if (connections == null) return null;

        return connections.stream()
                .filter(c -> c.getType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Получить целевой узел по типу соединения
     */
    public BaseNode getTargetByType(BaseNode source, ConnectionType type) {
        Connection connection = getConnectionByType(source, type);
        return connection != null ? connection.getTarget() : null;
    }

    /**
     * Проверить, есть ли у узла исходящие соединения
     */
    public boolean hasOutgoingConnections(BaseNode node) {
        if (node == null) return false;
        List<Connection> connections = outgoingConnections.get(node);
        return connections != null && !connections.isEmpty();
    }

    /**
     * Проверить, есть ли у узла входящие соединения
     */
    public boolean hasIncomingConnections(BaseNode node) {
        if (node == null) return false;
        List<Connection> connections = incomingConnections.get(node);
        return connections != null && !connections.isEmpty();
    }

    /**
     * Получить количество исходящих соединений
     */
    public int getOutgoingConnectionCount(BaseNode node) {
        return getOutgoingConnections(node).size();
    }

    /**
     * Получить количество входящих соединений
     */
    public int getIncomingConnectionCount(BaseNode node) {
        return getIncomingConnections(node).size();
    }

    /**
     * Очистить все соединения
     */
    public void clear() {
        // Удалить все стрелки с канваса
        for (List<Connection> connections : outgoingConnections.values()) {
            for (Connection conn : connections) {
                canvas.getChildren().remove(conn.getArrow());
            }
        }

        outgoingConnections.clear();
        incomingConnections.clear();
    }

    /**
     * Получить общее количество соединений
     */
    public int getTotalConnectionCount() {
        return outgoingConnections.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Получить все узлы, участвующие в соединениях
     */
    public Set<BaseNode> getAllConnectedNodes() {
        Set<BaseNode> nodes = new HashSet<>();
        nodes.addAll(outgoingConnections.keySet());
        nodes.addAll(incomingConnections.keySet());
        return nodes;
    }
}