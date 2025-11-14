package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.connection.ConType;
import com.alensoft.automator42.model.connection.Connect;
import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Step;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TableView {

    private final Canvas canvas;
    private final Map<Step, Integer> mapY = new LinkedHashMap<>();
    private final Map<Step, Integer> mapX = new LinkedHashMap<>();
    private Step[][] table = new Step[0][0];

    public TableView(Canvas canvas) {
        this.canvas = canvas;
    }

    public void updateLayout(Step root) {
        if (root == null) return;
        mapY.clear();
        mapX.clear();
        int maxDepth = calcY(root, 0);
        alignY(root);
        table = new Step[maxDepth + 1][2 * mapY.size() + 1];
        calcX(root, 0);
        int maxCount = mapY.size();
        while (alignX(root) && 0 < maxCount) {
            System.out.println("alignX #" + maxCount);
            maxCount--;
        }
        draw(root);
    }


    private void draw(Step root) {
        for (int y = 0; y < table.length; y++) {
            Step[] row = table[y];
            for (int x = 0; x < row.length; x++) {
                Step node = row[x];
                if (node == null) continue;
                if (!canvas.getChildren().contains(node)) {
                    canvas.getChildren().add(node);
                }
                node.setLayoutX(root.getLayoutX() + x * (Step.WIDTH + Step.STEP));
                node.setLayoutY(root.getLayoutY() + y * (Step.HEIGHT + Step.STEP));
            }
        }
    }


    private int calcY(Step node, int currentY) {
        int max = 0;
        int y = Math.max(currentY, mapY.getOrDefault(node, -1));
        mapY.put(node, y);
        for (Connect connect : node.getNextConnects()) {
            int nextMax = calcY(connect.getTarget(), currentY + 1);
            max = Math.max(max, nextMax);
        }
        return Math.max(currentY, max);
    }

    private void alignY(Step node) {
        for (Connect connect : node.getNextConnects()) {
            Step target = connect.getTarget();
            alignY(target);
            if (!(node instanceof Branch)) {
                mapY.put(node, mapY.get(target) - 1);
            }
        }
    }

    private void calcX(Step node, int currentX) {
        int y = mapY.get(node);
        Integer x = mapX.get(node);
        if (x == null) {
            while (table[y][currentX] != null) currentX++;
            table[y][currentX] = node;
            mapX.put(node, currentX);
        }
        List<Connect> nextConnects = node.getNextConnects();
        for (int i = 0, nextConnectsSize = nextConnects.size(); i < nextConnectsSize; i++) {
            Connect connect = nextConnects.get(i);
            calcX(connect.getTarget(), currentX + i);
        }
    }

    private boolean alignX(Step node) {
        boolean swap = false;
        final int x = mapX.get(node);
        for (Connect connect : node.getNextConnects()) {
            Step target = connect.getTarget();
            swap |= alignX(target);
            final Integer childX = mapX.get(target);
            if (connect.getType() == ConType.OK && childX != null && childX != x) {
                swap |= moveX(node, childX);
            }
        }
        return swap;
    }

    private boolean moveX(Step step, int newX) {
        Integer y = mapY.get(step);
        Integer x = mapX.get(step);
        if (y == null || y > table.length) {
            return false;
        }
        Step[] row = table[y];
        if (x == null || x >= row.length || newX >= row.length) {
            return false;
        }
        Step randomStep = row[newX];
        row[x] = randomStep;
        mapX.put(randomStep, x);
        row[newX] = step;
        mapX.put(step, newX);
        return true;
    }
}
