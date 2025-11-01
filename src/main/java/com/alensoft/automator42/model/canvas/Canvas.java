package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.connection.ConManager;
import com.alensoft.automator42.model.connection.ConType;
import com.alensoft.automator42.model.connection.Connect;
import com.alensoft.automator42.model.step.*;
import javafx.scene.layout.Pane;

import java.util.*;

public class Canvas extends Pane {

    private final Begin root;
    private Step selectedStep;
    private final ConManager conManager;

    public Canvas(int x, int y) {
        this.setPrefSize(1000, 700);
        this.setStyle("-fx-background-color: linear-gradient(#f8f8f8, #e8eef8);");
        conManager = new ConManager(this);

        root = new Begin("Start");
        root.relocate(x, y);
        getChildren().add(root);

        End end = new End("End");
        addStep(root, end);
        selectedStep = root;
    }

    public Step getSelectedStep() {
        return selectedStep;
    }

    public void setSelectedStep(Step currentStep) {
        this.selectedStep = currentStep;
    }

    public ConManager getConManager() {
        return conManager;
    }

    // ============= ДОБАВЛЕНИЕ УЗЛОВ =============

    /**
     * Добавить узел в основную цепочку (MAIN flow)
     */
    public Step addStep(final Step prev, final Step step) {
        return newStep(prev, step, ConType.DOWN);
    }


    public Step insertInBranch(final Step branch, final Step step) {
        if (!(branch instanceof Branch)) {
            throw new IllegalArgumentException("Step must be a Decision");
        }
        var optCon = conManager.getConByType(branch, ConType.EMPTY);
        ConType outType;
        if (optCon.isPresent()) {
            outType = ConType.MERGE;
        } else {
            outType = ConType.DOWN;
            optCon = conManager.getConByType(branch, ConType.BRANCH);
        }
        Connect con = optCon.orElseThrow();
        Step next = con.getTarget();
        conManager.removeCon(con);
        conManager.createCon(step, next, outType);
        newStep(branch, step, ConType.BRANCH);
        return step;
    }


    // ============= ВНУТРЕННЯЯ ЛОГИКА ВСТАВКИ =============

    /**
     * Универсальная вставка узла с сохранением AST
     */
    private Step newStep(final Step prev, final Step step, ConType insertionType) {
        if (prev == null || step == null) {
            throw new IllegalArgumentException("Steps cannot be null");
        }
        getChildren().add(step);

        Connect prevCon = conManager.getConByType(prev, insertionType, ConType.MERGE).orElse(null);
        // Найти соединение от prev
        if (prevCon != null) {
            // Есть следующий узел - вставляемся между ними
            Step nextStep = prevCon.getTarget();
            // Переподключить: prev -> step -> next
            conManager.removeCon(prevCon);
            conManager.createCon(prev, step, insertionType);
            conManager.createCon(step, nextStep, prevCon.getType()); //BRANCH or DOWN or MERGE
        } else {
            // Нет следующего узла - такого быть не может, но пока есть на старте
            conManager.createCon(prev, step, insertionType);
        }
        if (step instanceof Branch) {
            var down = conManager.getConByType(step, ConType.DOWN, ConType.MERGE);
            Step next = down.orElseThrow().getTarget();
            conManager.createCon(step, next, ConType.EMPTY);
        }
        update();
        return step;
    }


    // ============= УДАЛЕНИЕ УЗЛОВ =============
    public void removeStep(Step step) {
        if (step == null) return;

        // Нельзя удалить Begin и End
        if (step instanceof Begin || step instanceof End) {
            throw new IllegalArgumentException("Cannot remove Begin/End step");
        }

        // Получить входящие и исходящие соединения
        List<Connect> incoming = step.in();
        List<Connect> outgoing = step.out();

        // Если узел - Decision, удалить всю его NO ветку
        if (step instanceof Branch) {
            var optCon = conManager.getConByType(step, ConType.EMPTY);
            if (optCon.isEmpty()) {
                optCon = conManager.getConByType(step, ConType.BRANCH);
                if (optCon.isPresent()) {
                    Connect con = optCon.get();
                    Step target = con.getTarget();
                    while (target != null && target != step && target.in().size() == 1 && !target.out().isEmpty()) {
                        conManager.removeCon(con);
                        con = target.out().getFirst();
                        removeStep(target);
                        target = con.getTarget();
                    }
                    conManager.removeCon(con);
                }
            } else {
                conManager.removeCon(optCon.get());
            }
        }

        // переподключить соседей
        if (!incoming.isEmpty() && !outgoing.isEmpty()) {
            reconnectNeighbors(incoming, outgoing);
        }

        // Удалить все соединения и сам узел
        conManager.removeAllCons(step);
        getChildren().remove(step);
        update();
        // Обновить lastStep если удалили его
        if (selectedStep == step) {
            selectedStep = findNewLastStep();
        }
    }


    private void reconnectNeighbors(List<Connect> incoming, List<Connect> outgoing) {
        Connect out = outgoing.stream()
                .filter(con -> con.getType() == ConType.DOWN || con.getType() == ConType.MERGE)
                .findFirst()
                .orElseThrow();
        for (Connect in : incoming) {
            Step source = in.getSource();
            ConType inType = in.getType();
            Step target = out.getTarget();
            if (inType == ConType.BRANCH && out.getType() == ConType.MERGE) {
                inType = ConType.EMPTY;
            }
            try {
                conManager.createCon(source, target, inType);
            } catch (IllegalArgumentException e) {
                // Соединение уже существует - игнорируем
            }
        }
    }


    private void visit(Step step, Set<Step> visited, Queue<Step> queue) {
        for (Connect conn : step.out()) {
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
                .filter(step -> conManager.getConByType(step, ConType.DOWN) == null)
                .filter(step -> !(step instanceof Connector)) // Не коннекторы
                .findFirst()
                .orElse(null);
    }

// ============= СЛУЖЕБНЫЕ МЕТОДЫ =============


    /**
     * Проверить валидность AST (все узлы связаны, от Begin можно дойти до End)
     */
    public boolean validateAST() {
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

    public void update() {
        GraphLayoutUpdater.updateLayout(root);
        //update(startNode, startNode.getLayoutX(), startNode.getLayoutY(), new HashSet<>());
    }


    private void update(Step current, double x, double y, Set<Step> visited) {
        if (current == null || !visited.add(current)) {
            return;
        }

        current.relocate(x, y);

        Optional<Connect> mainCon = conManager.getConByType(current, ConType.DOWN);
        mainCon.ifPresent(con -> update(con.getTarget(), x, y + Step.HEIGHT + Step.STEP, visited));

        if (current instanceof Branch) {
            Optional<Connect> branchCon = conManager.getConByType(current, ConType.BRANCH);
            branchCon.ifPresent(con -> update(con.getTarget(), x + Step.WIDTH, y + Step.HEIGHT + Step.STEP, visited));
        }
    }
}