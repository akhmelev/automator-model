package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.connection.Connection;
import com.alensoft.automator42.model.connection.ConnectionManager;
import com.alensoft.automator42.model.connection.ConnectionType;
import com.alensoft.automator42.model.step.*;
import javafx.scene.layout.Pane;

import java.util.*;

public class Canvas extends Pane {

    private Step selectedStep;
    private final ConnectionManager connectionManager;

    public Canvas(int x, int y) {
        this.setPrefSize(1000, 700);
        this.setStyle("-fx-background-color: linear-gradient(#f8f8f8, #e8eef8);");

        connectionManager = new ConnectionManager(this);

        Begin begin = new Begin("Start");
        begin.relocate(x, y);
        getChildren().add(begin);

        End end = new End("End");
        addStep(begin, end);
        selectedStep = begin;
    }

    public Step getSelectedStep() {
        return selectedStep;
    }

    public void setSelectedStep(Step currentStep) {
        this.selectedStep = currentStep;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    // ============= ДОБАВЛЕНИЕ УЗЛОВ =============

    /**
     * Добавить узел в основную цепочку (MAIN flow)
     */
    public Step addStep(final Step prev, final Step step) {
        return createStep(prev, step, ConnectionType.DOWN);
    }


    public Step addBranch(final Step prev, final Step branch) {
        if (!(branch instanceof Branch)) {
            throw new IllegalArgumentException("Step must be a Decision");
        }
        createStep(prev, branch, ConnectionType.DOWN);
        var down = connectionManager.getConnectionByType(branch, ConnectionType.DOWN);
        Step next = down.orElseThrow().getTarget();
        connectionManager.createConnection(branch, next, ConnectionType.EMPTY);
        return branch;
    }

    public Step insertInBranch(final Step branch, final Step step) {
        if (!(branch instanceof Branch)) {
            throw new IllegalArgumentException("Step must be a Decision");
        }
        var optConnection = connectionManager.getConnectionByType(branch, ConnectionType.EMPTY);
        ConnectionType outType;
        if (optConnection.isPresent()) {
            outType = ConnectionType.MERGE;
        } else {
            outType = ConnectionType.DOWN;
            optConnection = connectionManager.getConnectionByType(branch, ConnectionType.BRANCH);
        }
        Connection connection = optConnection.orElseThrow();
        Step next = connection.getTarget();
        connectionManager.removeConnection(connection);
        connectionManager.createConnection(step, next, outType);
        createStep(branch, step, ConnectionType.BRANCH);
        return step;
    }


    // ============= ВНУТРЕННЯЯ ЛОГИКА ВСТАВКИ =============

    /**
     * Универсальная вставка узла с сохранением AST
     */
    private Step createStep(final Step prev, final Step step, ConnectionType insertionType) {
        if (prev == null || step == null) {
            throw new IllegalArgumentException("Steps cannot be null");
        }

        int fullStep = Step.HEIGHT + Step.STEP;

        // Добавить узел на canvas
        getChildren().add(step);

        double layoutX = prev.getLayoutX();
        double layoutY = prev.getLayoutY();
        Connection prevConnection = connectionManager.getConnectionByType(prev, insertionType).orElse(null);
        if (insertionType != ConnectionType.BRANCH) {
            step.relocate(layoutX, layoutY + fullStep);
        } else {
            step.relocate(layoutX + 200, layoutY);

        }
        // Найти соединение от prev
        if (prevConnection != null) {
            // Есть следующий узел - вставляемся между ними
            Step nextStep = prevConnection.getTarget();

            // Сдвинуть все узлы вниз от точки вставки
            shiftStepsDown(nextStep, fullStep);

            // Переподключить: prev -> step -> next
            connectionManager.removeConnection(prevConnection);
            connectionManager.createConnection(prev, step, insertionType);
            connectionManager.createConnection(step, nextStep, ConnectionType.DOWN);
        } else {
            // Нет следующего узла - такого быть не может, но пока есть на старте
            connectionManager.createConnection(prev, step, insertionType);
            shiftStepsDown(step, fullStep);
        }

        return step;
    }


    /**
     * Сдвинуть узел и все зависимые от него узлы вниз
     * Использует BFS для обхода всех потомков
     */
    private void shiftStepsDown(Step startStep, double offset) {
        if (startStep == null) return;

        Set<Step> visited = new HashSet<>();
        Queue<Step> queue = new LinkedList<>();

        queue.add(startStep);
        visited.add(startStep);

        while (!queue.isEmpty()) {
            Step current = queue.poll();
            current.setLayoutY(current.getLayoutY() + offset);

            // Добавить всех потомков
            visit(current, visited, queue);
        }
    }

    // ============= УДАЛЕНИЕ УЗЛОВ =============

    /**
     * Удалить узел с сохранением связей (reconnect соседей)
     */
    public void removeStep(Step step) {
        if (step == null) return;

        // Нельзя удалить Begin
        if (step instanceof Begin || step instanceof End) {
            throw new IllegalArgumentException("Cannot remove Begin/End step");
        }

        // Получить входящие и исходящие соединения
        List<Connection> incoming = step.in();
        List<Connection> outgoing = step.out();

        // Если узел - Decision, удалить всю его NO ветку
        if (step instanceof Branch) {
            var emptyLoop = connectionManager.getConnectionByType(step, ConnectionType.EMPTY);
            if (emptyLoop.isEmpty()) {
                Connection connection = connectionManager
                        .getConnectionByType(step, ConnectionType.BRANCH)
                        .orElseThrow();
                Step target = connection.getTarget();
                while (target != null && target != step && target.in().size() == 1) {
                    connectionManager.removeConnection(connection);
                    removeStep(target);
                    connection = target.out().getFirst();
                    target = connection.getTarget();
                }
                connectionManager.removeConnection(connection);
            } else {
                connectionManager.removeConnection(emptyLoop.get());
            }
        }

        // переподключить соседей
        if (!incoming.isEmpty() && !outgoing.isEmpty()) {
            reconnectNeighbors(incoming, outgoing);
        }

        // Сдвинуть узлы вверх после удаления
        if (!outgoing.isEmpty()) {
            int fullStep = Step.HEIGHT + Step.STEP;
            for (Connection conn : outgoing) {
                shiftStepsUp(conn.getTarget(), fullStep);
            }
        }

        // Удалить все соединения и сам узел
        connectionManager.removeAllConnections(step);
        getChildren().remove(step);

        // Обновить lastStep если удалили его
        if (selectedStep == step) {
            selectedStep = findNewLastStep();
        }
    }


//    /**
//     * Удалить Decision вместе с его содержимым
//     */
//    private void removeNoBranch(Step decision) {
//        Connection branch = connectionManager.getConnectionByType(decision, ConnectionType.BRANCH);
//        Step target = branch.getTarget();
//        while (target != null && target != decision && target.in().size() == 1) {
//            removeStep(target);
//            target = branch.getTarget();
//        }
//    }

    /**
     * Переподключить соседей при удалении узла
     */
    private void reconnectNeighbors(List<Connection> incoming, List<Connection> outgoing) {
        for (Connection in : incoming) {
            Step source = in.getSource();
            ConnectionType inType = in.getType();

            for (Connection out : outgoing) {
                Step target = out.getTarget();
                ConnectionType outType = out.getType();

                // Сохранить тип соединения (приоритет у нестандартного типа)
                ConnectionType reconnectType = inType != ConnectionType.DOWN
                        ? (
                        //но при слиянии проверяем пустые ветки
                        inType == ConnectionType.BRANCH && outType == ConnectionType.MERGE
                                ? ConnectionType.EMPTY
                                : inType)
                        :
                        outType;

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
    private void shiftStepsUp(Step startStep, double offset) {
        if (startStep == null) return;

        Set<Step> visited = new HashSet<>();
        Queue<Step> queue = new LinkedList<>();

        queue.add(startStep);
        visited.add(startStep);

        while (!queue.isEmpty()) {
            Step current = queue.poll();
            current.setLayoutY(current.getLayoutY() - offset);

            visit(current, visited, queue);
        }
    }

    private void visit(Step step, Set<Step> visited, Queue<Step> queue) {
        for (Connection conn : step.out()) {
            Step child = conn.getTarget();
            if (!visited.contains(child)) {
                visited.add(child);
                queue.add(child);
            }
        }
    }

    /**
     * Найти новый lastStep после удаления текущего
     */
    private Step findNewLastStep() {
        return getChildren().stream()
                .filter(step -> step instanceof Step)
                .map(step -> (Step) step)
                .filter(step -> connectionManager.getConnectionByType(step, ConnectionType.DOWN) == null)
                .filter(step -> !(step instanceof Connector)) // Не коннекторы
                .findFirst()
                .orElse(null);
    }

// ============= СЛУЖЕБНЫЕ МЕТОДЫ =============


    /**
     * Проверить валидность AST (все узлы связаны, от Begin можно дойти до End)
     */
    public boolean validateAST() {
        Step root = getRootStep();
        if (root == null) return false;

        Set<Step> reachable = new HashSet<>();
        Queue<Step> queue = new LinkedList<>();

        queue.add(root);
        reachable.add(root);

        while (!queue.isEmpty()) {
            Step current = queue.poll();
            visit(current, reachable, queue);
        }

        // Все узлы должны быть достижимы из Begin
        long totalSteps = getChildren().stream()
                .filter(step -> step instanceof Step)
                .count();

        return reachable.size() == totalSteps;
    }

    /**
     * Получить корневой узел (Begin)
     */
    public Step getRootStep() {
        return getChildren().stream()
                .filter(step -> step instanceof Begin)
                .map(step -> (Step) step)
                .findFirst()
                .orElse(null);
    }
}