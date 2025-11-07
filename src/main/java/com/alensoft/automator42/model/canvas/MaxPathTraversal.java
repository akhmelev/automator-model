package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.step.Step;

import java.util.*;

class MaxPathTraversal {

    private static final Map<Step, Integer> widthSubTree = new HashMap<>(); // Для расчета X
    private static final Deque<Step> queue = new LinkedList<>(); // Для обхода

    public static void updateLayout(Step root) {
        if (root == null) return;
        calculateSubtreeWidth(root);
        queue.add(root);
        bfs(0, 0);
    }

    protected static void bfs(int row, int col) {
        Step step = queue.poll();
        if (step == null) return;
        step.setLayoutX(row * (Step.WIDTH + Step.STEP));
        step.setLayoutY(col * (Step.HEIGHT + Step.STEP));
        for (int i = 0; i < step.getNextSteps().size(); i++) {
            
        }
    }

    private static int calculateSubtreeWidth(Step node) {
        int w = 0;
        for (Step nextStep : node.getNextSteps()) {
            w += calculateSubtreeWidth(nextStep);
        }
        w = Math.max(1, w);
        widthSubTree.put(node, w);
        return w;
    }


}