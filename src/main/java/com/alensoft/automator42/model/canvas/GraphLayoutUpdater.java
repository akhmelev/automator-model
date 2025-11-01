package com.alensoft.automator42.model.canvas;


import com.alensoft.automator42.model.step.Step;

import java.util.*;

class GraphLayoutUpdater {

    private static class StepData {
        int gridX = -1;
        int gridY = -1;
    }

    private static class LayoutResult {
        final int nextY; // Следующая доступная Y-координата (самая низкая точка)
        final int maxX;  // Самый правый использованный X в подграфе
        LayoutResult(int nextY, int maxX) {
            this.nextY = nextY;
            this.maxX = maxX;
        }
    }

    private static Map<Step, StepData> dataMap;
    private static Set<Step> visited;
    private static Set<Step> mergePoints;

    public static void updateLayout(Step root) {
        if (root == null) return;

        dataMap = new HashMap<>();
        mergePoints = new HashSet<>();

        collectAllStepsAndMerges(root);

        visited = new HashSet<>();
        placeRecursive(root, 0, 0);

        applyLayoutCoordinates(root, dataMap);
    }

    // --- Шаг 1: Сбор данных и точек слияния ---
    private static void collectAllStepsAndMerges(Step step) {
        if (dataMap.containsKey(step)) return;
        dataMap.put(step, new StepData());

        for (Step child : step.getNextSteps()) {
            if (dataMap.containsKey(child)) {
                mergePoints.add(child);
            }
            collectAllStepsAndMerges(child);
        }
    }

    // --- Шаг 2: Рекурсивная раскладка (DFS) ---
    private static LayoutResult placeRecursive(Step current, int currentX, int currentY) {
        StepData currentData = dataMap.get(current);

        // A. Слияние (Повторное посещение):
        if (visited.contains(current)) {
            // Если мы пришли сюда второй раз, это узел слияния.
            // Обновляем его Y, если текущая ветка длиннее.
            if (currentY > currentData.gridY) {
                currentData.gridY = currentY;
            }
            // Возвращаем его Y и X (без изменения).
            // Это гарантирует, что следующая боковая ветка начнет выравниваться с этой точкой.
            return new LayoutResult(currentData.gridY, currentX);
        }
        visited.add(current);

        // 1. Размещение (X и Y)
        currentData.gridX = currentX;
        currentData.gridY = currentY;

        // 2. Конечный узел
        if (current.getNextSteps().isEmpty()) {
            return new LayoutResult(currentY + 1, currentX);
        }

        List<Step> children = current.getNextSteps();
        int nextY = currentY + 1;

        // 3. Обработка Главной/Текущей Ветки (Индекс 0)
        Step primaryChild = children.get(0);
        LayoutResult primaryResult = placeRecursive(primaryChild, currentX, nextY);

        int nextAvailableY = primaryResult.nextY; // Y после главной ветки
        int maxX = primaryResult.maxX;

        // 4. Обработка Боковых Веток (Индекс 1+)
        if (children.size() > 1) {

            // X для первой боковой ветки:
            // Самая правая точка, достигнутая в основной ветке (maxX) + 1 (зазор).
            int nextBranchX = maxX + 1;

            for (int i = 1; i < children.size(); i++) {
                Step sideChild = children.get(i);

                LayoutResult sideResult = placeRecursive(sideChild, nextBranchX, nextY);

                // Обновляем Y: берем самый низкий Y
                nextAvailableY = Math.max(nextAvailableY, sideResult.nextY);

                // Обновляем X для следующей боковой ветки:
                nextBranchX = sideResult.maxX + 1;
            }
            // Обновляем maxX: он равен самой правой точке, достигнутой в боковых ветках
            maxX = nextBranchX - 1;
        }

        // 5. Размещение узла слияния (`end`)
        // Если следующий узел является слиянием, устанавливаем его Y на nextAvailableY.
        // Это гарантирует, что end не "убежит" вверх.
        Step nextStep = primaryChild;
        if (mergePoints.contains(nextStep)) {
            StepData mergeData = dataMap.get(nextStep);
            if (mergeData.gridY == -1 || nextAvailableY > mergeData.gridY) {
                // ПРИСВОЕНИЕ: Узел слияния получает самый низкий Y
                mergeData.gridY = nextAvailableY;
            }
        } else {
            // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Если следующий узел НЕ слияние,
            // он уже был размещен главной веткой. Нам не нужно тут ничего делать.
        }

        // 6. Возврат
        // Возвращаем самый низкий Y и самый правый X
        return new LayoutResult(nextAvailableY, maxX);
    }


    // --- Шаг 3: Применение координат ---
    private static void applyLayoutCoordinates(Step root, Map<Step, StepData> dataMap) {
        Set<Step> allSteps = dataMap.keySet();

        final double X_STEP = 130.0;
        final double Y_STEP = Step.HEIGHT + Step.STEP;

        for (Step step : allSteps) {
            StepData data = dataMap.get(step);

            if (data.gridX != -1 && data.gridY != -1) {
                step.setLayoutX(root.getLayoutX() + data.gridX * X_STEP);
                step.setLayoutY(root.getLayoutY() + data.gridY * Y_STEP);
            }
        }
    }
}