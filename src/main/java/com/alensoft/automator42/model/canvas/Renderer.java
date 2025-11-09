package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.connection.ConType;
import com.alensoft.automator42.model.connection.Connect;
import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Null;
import com.alensoft.automator42.model.step.Step;

import java.util.*;

class Renderer {

    private final Canvas canvas;
    private final Map<Step, Integer> depth = new LinkedHashMap<>();
    private final Map<Step, Integer> branchCount = new LinkedHashMap<>();
    private final Map<Integer, List<Step>> lines = new TreeMap<>();

    public Renderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void updateLayout(Step root) {
        if (root == null) return;
        branchCount.clear();
        depth.clear();
        lines.clear();
        canvas.getChildren().removeIf(child -> child instanceof Null);
        calcY(root, 0);
        calcX(root, 0);
        int alignCount = 0;
        while (alignCount < depth.size() && alignX(root)) {
            alignCount++;
        }
        draw(root);
    }

    private void draw(Step root) {
        for (Map.Entry<Integer, List<Step>> lineData : lines.entrySet()) {
            Integer y = lineData.getKey();
            List<Step> line = lineData.getValue();
            for (int x = 0; line != null && x < line.size(); x++) {
                Step node = line.get(x);
                if (node == null) continue;
                if (!canvas.getChildren().contains(node)) {
                    canvas.getChildren().add(node);
                    node.toBack();
                }
                node.setLayoutX(root.getLayoutX() + x * (Step.WIDTH / 2. + Step.STEP));
                node.setLayoutY(root.getLayoutY() + y * (Step.HEIGHT + Step.STEP));
            }
        }
    }


    private void calcY(Step node, int level) {
        int y = Math.max(level, depth.getOrDefault(node, -1));
        depth.put(node, y);
        int n = branchCount.getOrDefault(node, 0);
        for (Connect connect : node.getNextConnects()) {
            branchCount.put(node, n);
            calcY(connect.getTarget(), level + 1);
            n++;
        }
        int in = node.getPreviousConnects().size();
        n -= in;
        branchCount.put(node, n);
    }

    private void calcX(Step node, int x) {
        List<Step> line = getLine(node);
        if (!line.contains(node)) {
            x = grow(line, x);
            line.add(node);
            line.add(new Null());
        }
        List<Connect> nextConnects = node.getNextConnects();
        for (int i = 0, nextConnectsSize = nextConnects.size(); i < nextConnectsSize; i++) {
            Connect connect = nextConnects.get(i);
            if (connect.getType() == ConType.OK || connect.getType() == ConType.IN)
                calcX(connect.getTarget(), x + i);
//            if (i > 0 && node instanceof Branch && node.getNextConnects().get(1).getType() != ConType.EMPTY) {
//                int nextLineEnd = lines.computeIfAbsent(depth.get(node) + 1, k -> new ArrayList<>()).size();
//                do {
//                    Null ifElse = new Null();
//                    line.add(ifElse);
//                } while (line.size() < nextLineEnd);
//            }
        }
    }

    private int grow(List<Step> line, int x) {
        if (line.size() > x) return line.size();
        while (line.size() < x) {
            Null nul = new Null();
            canvas.getChildren().add(nul);
            line.add(nul);
        }
        return x;
    }

//    private Step getEnd(Step root) {
//        return depth.entrySet()
//                .stream()
//                .sorted(Map.Entry.comparingByValue())
//                .skip(depth.size() - 1)
//                .map(Map.Entry::getKey)
//                .findFirst()
//                .orElse(root);
//    }

    private boolean alignX(Step node) {
        boolean result = alignByFirstChild(node);
        result |= alignSecondChild(node);
        for (Connect nextConnect : node.getNextConnects()) {
            result |= alignX(nextConnect.getTarget());
        }
        return result;
    }

    private boolean alignByFirstChild(Step node) {
        List<Connect> nextConnects = node.getNextConnects();
        if (nextConnects == null || nextConnects.isEmpty()) return false;
        Connect child = nextConnects.getFirst();
        if (child.getType() != ConType.OK) return false;
        int targetX = getX(child.getTarget());
        boolean result = false;
        while (getX(node) < targetX) {
            int x = getX(node);
            getLine(node).add(x, new Null());
            result = true;
        }
        return result;
    }

    private boolean alignSecondChild(Step node) {
        boolean result = false;
        if (!(node instanceof Branch)) return false;
        List<Connect> nextConnects = node.getNextConnects();
        if (nextConnects == null || nextConnects.size() < 2) return false;
        Connect elseChild = nextConnects.get(1);
        if (elseChild.getType() != ConType.IN) return false;
        int ifX = getX(node);
        Step target = elseChild.getTarget();
        int branchX = getX(target);
        while (ifX >= branchX) {
            getLine(target).add(branchX, new Null());
            branchX = getX(target);
            result = true;
        }
        return result;
    }

    private int getX(Step node) {
        List<Step> line = getLine(node);
        for (int i = 0; i < line.size(); i++) {
            if (line.get(i) == node) {
                return i;
            }
        }
        return 0;
    }

    private List<Step> getLine(Step node) {
        int y = depth.get(node);
        return lines.computeIfAbsent(y, k -> new ArrayList<>());
    }
}