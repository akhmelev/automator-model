package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.connection.ConType;
import com.alensoft.automator42.model.connection.Connect;
import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Null;
import com.alensoft.automator42.model.step.Step;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.function.Predicate;

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
        while (true) {
            if (alignX(root, this::alignBody)) continue;
            if (alignX(root, this::alignIf)) continue;
            if (alignX(root, this::alignElse)) continue;
            if (alignX(root, this::alignOut)) continue;
            break;
        }
//        compaction();
        draw(root);
    }

    private void compaction() {
        int maxDepth = depth.values()
                .stream()
                .max(Comparator.comparingInt(Integer::intValue))
                .orElse(-1);
        int maxX = -1;
        for (int y = 0; y <= maxDepth; y++) {
            maxX = Math.max(maxX, lines.get(y).size());
        }

        for (int x = 1; x < maxX; ) {
            boolean deleteCol = true;
            for (int y = 0; y <= maxDepth; y++) {
                List<Step> line = lines.get(y);
                if (line.size() <= x || (!(line.get(x) instanceof Null) && !(line.get(x - 1) instanceof Null))) {
                    deleteCol = false;
                }
            }
            if (deleteCol) {
                for (int y = 0; y <= maxDepth; y++) {
                    List<Step> line = lines.get(y);
                    if (line.size() > x && (line.get(x) instanceof Null)) {
                        line.remove(x);
                    }
                }
            } else {
                x++;
            }
        }
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
        if (!line.contains(node)) { //not found, add last
            x = grow(line, x);
            line.add(node);
            line.add(new Null(Color.WHITE));
        }
        List<Connect> nextConnects = node.getNextConnects();
        for (int i = 0; i < nextConnects.size(); i++) {
            Connect connect = nextConnects.get(i);
            if (connect.getType() == ConType.OK || connect.getType() == ConType.NO)
                calcX(connect.getTarget(), x + i); //recursive
        }
    }

    private int grow(List<Step> line, int x) {
        if (line.size() > x) return line.size();
        while (line.size() < x) {
            Null nul = new Null(Color.WHITE);
            canvas.getChildren().add(nul);
            line.add(nul);
        }
        return x;
    }

    private boolean alignX(Step node, Predicate<Step> operation) {
        boolean isMoved = false;
        isMoved |= operation.test(node);
        List<Connect> nextConnects = node.getNextConnects();
        for (Connect nextConnect : nextConnects) {
            isMoved |= alignX(nextConnect.getTarget(), operation);
        }
        return isMoved;
    }

    private boolean alignOut(Step node) {
        for (Connect nextConnect : node.getNextConnects()) {
            if (nextConnect.getType() == ConType.OUT) {
                int startY = depth.getOrDefault(node, -1) + 1;
                int stopY = depth.getOrDefault(nextConnect.getTarget(), -1) + 1;
                int x = getX(node);
                for (int y = startY; y < stopY; y++) {
                    List<Step> line = lines.computeIfAbsent(y, k -> new ArrayList<>());
                    if (line.size() > x && !(line.get(x) instanceof Null)) {
                        getLine(node).add(x, new Null(Color.RED));
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean alignIf(Step node) {
        List<Connect> nextConnects = node.getNextConnects();
        if (nextConnects == null || nextConnects.isEmpty()) return false;
        for (Connect connect : nextConnects) {
            if (connect.getType() == ConType.OK) {
                Step child = connect.getTarget();
                final int targetX = getX(child);
                int parentX = getX(node);
                if (parentX < targetX) {
                    List<Step> line = getLine(node);
                    line.add(parentX, new Null(Color.GREEN));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean alignBody(Step node) {
        List<Connect> nextConnects = node.getNextConnects();
        if (nextConnects == null || nextConnects.isEmpty()) return false;
        for (Connect connect : nextConnects) {
            if (connect.getType() == ConType.OK) {
                Step child = connect.getTarget();
                int targetX = getX(child);
                int parentX = getX(node);
                if (parentX > targetX) {
                    getLine(child).add(targetX, new Null(Color.SILVER));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean alignElse(Step node) {
        if (!(node instanceof Branch)) return false;

        Connect okConnect = null;
        Connect noConnect = null;
        for (Connect connect : node.getNextConnects()) {
            if (connect.getType() == ConType.OK) {
                okConnect = connect;
            } else if (connect.getType() == ConType.NO) {
                noConnect = connect;
            }
        }

        if (okConnect == null || noConnect == null) {
            return false; // Not a full if-else
        }

        Step okRoot = okConnect.getTarget();
        Step noRoot = noConnect.getTarget();

        Step mergeNode = findMergeNode(okRoot, noRoot);
        int maxX_in_ok_branch = findMaxXInSubtree(okRoot, mergeNode);
        int noRootX = getX(noRoot);

        if (noRootX <= maxX_in_ok_branch) {
            List<Step> noLine = getLine(noRoot);
            int shifts = maxX_in_ok_branch - noRootX + 1;
            for (int i = 0; i < shifts; i++) {
                noLine.add(noRootX, new Null(Color.YELLOW));
            }
            return true;
        }

        return false;
    }

    private Step findMergeNode(Step okRoot, Step noRoot) {
        if (okRoot == null || noRoot == null) return null;

        // 1. Collect all successors of okRoot
        Set<Step> okSuccessors = new HashSet<>();
        Queue<Step> q = new LinkedList<>();
        q.add(okRoot);
        Set<Step> visitedOk = new HashSet<>();
        visitedOk.add(okRoot);

        while (!q.isEmpty()) {
            Step current = q.poll();
            for (Connect next : current.getNextConnects()) {
                Step target = next.getTarget();
                if (target != null && !visitedOk.contains(target) && depth.containsKey(target) && depth.get(target) > depth.get(current)) {
                    okSuccessors.add(target);
                    visitedOk.add(target);
                    q.add(target);
                }
            }
        }

        if (okSuccessors.isEmpty()) return null;

        // 2. BFS from noRoot and find all common successors
        q.clear();
        q.add(noRoot);
        Set<Step> visitedNo = new HashSet<>();
        visitedNo.add(noRoot);
        List<Step> potentialMerges = new ArrayList<>();

        while (!q.isEmpty()) {
            Step current = q.poll();

            if (okSuccessors.contains(current)) {
                potentialMerges.add(current);
            }

            // Continue traversal to find all possible merge points
            for (Connect next : current.getNextConnects()) {
                Step target = next.getTarget();
                if (target != null && !visitedNo.contains(target) && depth.containsKey(target) && depth.get(target) > depth.get(current)) {
                    visitedNo.add(target);
                    q.add(target);
                }
            }
        }

        // Return the one with the minimum depth (the "highest" in the diagram)
        return potentialMerges.stream().min(Comparator.comparingInt(s -> depth.get(s))).orElse(null);
    }


    private int findMaxXInSubtree(Step root, Step stopNode) {
        if (root == null) {
            return -1;
        }

        int maxX = -1;
        Set<Step> visited = new HashSet<>();
        Queue<Step> queue = new LinkedList<>();

        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            Step current = queue.poll();

            if (current == stopNode) {
                continue;
            }

            maxX = Math.max(maxX, getX(current));

            for (Connect next : current.getNextConnects()) {
                Step target = next.getTarget();
                if (target != null && !visited.contains(target)) {
                    if (depth.containsKey(target) && depth.get(target) > depth.get(current)) {
                        visited.add(target);
                        queue.add(target);
                    }
                }
            }
        }
        return maxX;
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
