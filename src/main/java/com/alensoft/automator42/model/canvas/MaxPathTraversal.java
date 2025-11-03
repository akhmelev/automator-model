package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.step.Step;

import java.util.*;

class MaxPathTraversal {

    // Статические поля для хранения результатов обхода
    private static final Map<Step, Integer> maxDepths = new HashMap<>(); // Для расчета Y
    private static final Map<Step, Double> finalXMap = new HashMap<>(); // Для расчета X
    private static final Queue<Step> queue = new LinkedList<>(); // Для обхода

    // Метод, запускающий обход и устанавливающий координаты
    public static void updateLayout(Step root) {
        if (root == null) return;

        maxDepths.clear();
        finalXMap.clear();
        queue.clear();

        // 1. Расчет Y (Max Depth)
        calculateMaxDepth(root);

        // 2. Группировка по Y
        List<List<Step>> maxPathLayers = groupStepsByDepth();

        // 3. Расчет X (Дракон-схема) и применение координат
        assignXCoordinates(root, maxPathLayers);
    }

    // 1. Расчет Y (Max Depth) - Итеративный обход для поиска самого длинного пути
    public static List<List<Step>> getMaxPathLayers(Step root) {
        if (root == null) return Collections.emptyList();
        maxDepths.clear();
        queue.clear();
        calculateMaxDepth(root);
        return groupStepsByDepth();
    }

    private static void calculateMaxDepth(Step root) {
        maxDepths.put(root, 0);
        queue.offer(root);

        while (!queue.isEmpty()) {
            Step current = queue.poll();
            int currentDepth = maxDepths.get(current);

            for (Step next : current.getNextSteps()) {
                int newDepth = currentDepth + 1;

                if (newDepth > maxDepths.getOrDefault(next, -1)) {
                    maxDepths.put(next, newDepth);
                    queue.offer(next);
                }
            }
        }
    }

    // 2. Группировка узлов по их финальной максимальной глубине (Y)
    private static List<List<Step>> groupStepsByDepth() {
        if (maxDepths.isEmpty()) return Collections.emptyList();

        int maxDepth = maxDepths.values().stream().max(Integer::compare).orElse(-1);

        List<List<Step>> layers = new ArrayList<>();
        for (int i = 0; i <= maxDepth; i++) {
            layers.add(new ArrayList<>());
        }

        for (Map.Entry<Step, Integer> entry : maxDepths.entrySet()) {
            layers.get(entry.getValue()).add(entry.getKey());
        }

        return layers;
    }

    // 3. Расчет X (Дракон-схема) и применение X/Y координат
    private static void assignXCoordinates(Step root, List<List<Step>> layers) {
        double currentY = root.getLayoutY();
        double stepY = Step.HEIGHT + Step.STEP;
        double stepX = Step.WIDTH + Step.STEP;

        finalXMap.put(root, root.getLayoutX());
        Queue<Step> queueX = new LinkedList<>();
        queueX.offer(root);

        // Обход для установки X
        while (!queueX.isEmpty()) {
            Step current = queueX.poll();
            double baseX = finalXMap.get(current);
            double branchOffset = 0;

            for (int i = 0; i < current.getNextSteps().size(); i++) {
                Step next = current.getNextSteps().get(i);
                double nextX;

                if (i == 0) {
                    // Основной путь (индекс 0)
                    nextX = baseX;
                } else {
                    // Ответвления (индекс > 0)
                    branchOffset += stepX;
                    nextX = baseX + branchOffset;
                }

                // Обработка слияния:
                if (finalXMap.containsKey(next)) {
                    // Если новая X меньше (левее) уже установленной, обновляем
                    if (nextX < finalXMap.get(next)) {
                        finalXMap.put(next, nextX);
                        queueX.offer(next);
                    }
                } else {
                    // Узел посещен впервые
                    finalXMap.put(next, nextX);
                    queueX.offer(next);
                }
            }
        }

        // Применение X и Y к узлам
        for (List<Step> layer : layers) {
            for (Step step : layer) {
                step.setLayoutX(finalXMap.getOrDefault(step, root.getLayoutX()));
                step.setLayoutY(currentY);
            }
            currentY += stepY;
        }
    }
}