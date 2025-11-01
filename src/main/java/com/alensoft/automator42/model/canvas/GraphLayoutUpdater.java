package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.step.Step;

import java.util.*;

class GraphLayoutUpdater {

    private static class StepData {
        boolean visited = false;
        int branchId = -1;
        int inDegree = 0;
    }

    public static void updateLayout(Step root) {
        if (root == null) return;

        Map<Step, StepData> dataMap = new HashMap<>();
        Set<Step> allSteps = new LinkedHashSet<>();

        // 1. Обход DFS для сбора всех узлов и расчета inDegree (не изменился)
        collectAndCalculateDegrees(root, dataMap, allSteps);

        // 2. Многоуровневая топологическая сортировка (Кан) и присвоение layoutY (не изменился)

        Queue<Step> readyQueue = new LinkedList<>();
        for (Step step : allSteps) {
            if (dataMap.get(step).inDegree == 0) {
                readyQueue.add(step);
            }
        }

        final double Y_STEP = Step.HEIGHT + Step.STEP;
        double currentY = 0;
        List<Step> orderedSteps = new ArrayList<>();

        while (!readyQueue.isEmpty()) {
            int levelSize = readyQueue.size();
            Queue<Step> nextLevelQueue = new LinkedList<>();

            for (int i = 0; i < levelSize; i++) {
                Step current = readyQueue.poll();
                current.setLayoutY(root.getLayoutY() + currentY);
                orderedSteps.add(current);

                for (Step child : current.getNextSteps()) {
                    if (dataMap.containsKey(child)) {
                        StepData childData = dataMap.get(child);
                        childData.inDegree--;
                        if (childData.inDegree == 0) {
                            nextLevelQueue.add(child);
                        }
                    }
                }
            }
            readyQueue = nextLevelQueue;
            currentY += Y_STEP;
        }

        // 3. Определение layoutX (Без пересечений)

        // Map для отслеживания самой правой X-координаты, занятой на каждом Y-уровне.
        // Ключ: Y-координата, Значение: Самый правый используемый branchId.
        Map<Double, Integer> maxBranchIdAtY = new HashMap<>();

        // Map для хранения ID активных веток на каждом X-уровне
        Map<Integer, Double> branchIdToMaxY = new HashMap<>();

        // Максимальный BranchId, использованный до сих пор, для новых веток.
        int nextAvailableBranchId = 1;

        for (Step current : orderedSteps) {
            StepData currentData = dataMap.get(current);

            // 1. Определение branchId
            int branchId;

            if (current == root) {
                branchId = 0;
                currentData.branchId = 0;
            } else if (currentData.branchId == -1) {
                // 2. Если ID не назначен (новая ветка или слияние)

                // Находим максимально занятый ID на текущем уровне Y
                int maxUsedIdOnCurrentY = maxBranchIdAtY.getOrDefault(current.getLayoutY(), 0);

                // Новая ветка начинается за всеми уже размещенными на этом уровне,
                // или за всеми ранее использованными, смотря что правее.

                // Начинаем поиск новой линии с max(maxUsedIdOnCurrentY + 1, nextAvailableBranchId)
                branchId = Math.max(maxUsedIdOnCurrentY + 1, nextAvailableBranchId);

                currentData.branchId = branchId;
                nextAvailableBranchId = branchId + 1;
            } else {
                // 3. ID уже унаследован
                branchId = currentData.branchId;
            }

            // Обновляем maxBranchIdAtY для текущего уровня
            maxBranchIdAtY.put(current.getLayoutY(),
                    Math.max(maxBranchIdAtY.getOrDefault(current.getLayoutY(), 0), branchId));


            final double X_STEP = 130.0;
            current.setLayoutX(root.getLayoutX() + branchId * X_STEP);

            // 4. Распределение branchId потомкам (Наследование)
            if (!current.getNextSteps().isEmpty()) {
                List<Step> childrenList = current.getNextSteps();

                // 1. Наследуем ID первому потомку (ГЛАВНОЙ ЛИНИИ)
                Step primaryChild = childrenList.get(0);
                StepData primaryData = dataMap.get(primaryChild);

                // Если ID еще не назначен, он наследует ID родителя.
                if (primaryData.branchId == -1) {
                    primaryData.branchId = branchId;
                }

                // 2. Остальные потомки получают новый, самый правый ID
                for (int j = 1; j < childrenList.size(); j++) {
                    Step secondaryChild = childrenList.get(j);
                    StepData secondaryData = dataMap.get(secondaryChild);

                    if (secondaryData.branchId == -1) {
                        // Эта новая ветка (боковая) должна гарантированно
                        // находиться правее, чем все уже используемые ID, включая текущий.

                        // Берем новый, самый правый ID
                        int newBranchId = nextAvailableBranchId;

                        secondaryData.branchId = newBranchId;
                        nextAvailableBranchId = newBranchId + 1;
                    }
                }
            }
        }
    }

    private static void collectAndCalculateDegrees(Step step, Map<Step, StepData> dataMap, Set<Step> allSteps) {
        StepData data = dataMap.computeIfAbsent(step, k -> new StepData());

        if (data.visited) return;
        data.visited = true;

        allSteps.add(step);

        for (Step child : step.getNextSteps()) {
            if (!dataMap.containsKey(child)) {
                dataMap.put(child, new StepData());
            }
            dataMap.get(child).inDegree++;

            collectAndCalculateDegrees(child, dataMap, allSteps);
        }
    }
}