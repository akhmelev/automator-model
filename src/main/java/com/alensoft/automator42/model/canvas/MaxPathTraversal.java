package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.connection.Connect;
import com.alensoft.automator42.model.step.Step;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class MaxPathTraversal {

    private static final Map<Step, Integer> width = new HashMap<>(); // Для расчета X
    private static final Map<Step, Integer> depth = new HashMap<>();
    private static final Set<Step> visited = new HashSet<>();

    public static void updateLayout(Step root) {
        if (root == null) return;
        width.clear();
        depth.clear();
        visited.clear();
        calculateSubtreeWidth(root, 0);
        draw(root, root, 0);
    }

    protected static void draw(Step root, Step node, int col) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);
        node.setLayoutX(root.getLayoutX() + col * (Step.WIDTH + Step.STEP));
        node.setLayoutY(root.getLayoutY() + depth.get(node) * (Step.HEIGHT + Step.STEP));
        for (Step next : node.getNextSteps()) {
            draw(root, next, col);
            col = col + Math.max(1, width.get(next));
        }
    }

    private static int calculateSubtreeWidth(Step node, int level) {
        int w = 0;
        for (Connect connect : node.getNextConnects()) {
            int subW = calculateSubtreeWidth(connect.getTarget(), level + 1);
            w += Math.max(1, subW);
        }
        width.put(node, Math.max(1, w));
        depth.put(node, Math.max(level, depth.getOrDefault(node, -1)));
        return Math.max(1, w);
    }


}