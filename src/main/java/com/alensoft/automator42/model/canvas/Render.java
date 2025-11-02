package com.alensoft.automator42.model.canvas;

import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Step;

import java.util.*;

public class Render {

    private record Point(double x, double y, double width, double height, double maxY) {

        static Point of(double x, double y, double width, double height, double maxY) {
            return new Point(x, y, width, height, maxY);
        }

        static Point of(double x, double y) {
            return new Point(x, y, 0, 0, y);
        }
    }

    public static void updateLayout(Step root) {
        Set<Step> visited = new HashSet<>();
        Deque<Step> waiting = new ArrayDeque<>();

        Point currentPoint = Point.of(root.getLayoutX(), root.getLayoutY());

        // 1. Рекурсивная отрисовка основной структуры и заполнение очереди
        Point resultPoint = draw(root, currentPoint, visited, waiting);

        // --- Исправление #1: Учет максимального Y для начала отрисовки очереди ---
        // Начальная Y-координата для очереди слияний - это maxY последнего нарисованного узла.
        double startYForMerge = resultPoint.maxY() + Step.STEP;

        // Обновляем текущую позицию Y
        currentPoint = Point.of(resultPoint.x(),
                startYForMerge,
                Step.WIDTH,
                0,
                startYForMerge);

        // 2. Отрисовка узлов слияния ("шампур" из точек слияния)
        while (!waiting.isEmpty()) {
            Step mergeNode = waiting.pollFirst();

            // Если узел уже нарисован (посещен), просто сдвигаем Y под него
            if (mergeNode.getLayoutY() != 0 || mergeNode.getLayoutX() != 0) {
                currentPoint = Point.of(currentPoint.x(),
                        mergeNode.getLayoutY() + Step.HEIGHT + Step.STEP,
                        Step.WIDTH,
                        0,
                        mergeNode.getLayoutY() + Step.HEIGHT);
                continue;
            }

            // --- Исправление #2: Гарантированная установка координат ---
            // Отрисовываем узел слияния, который не был отрисован (он должен быть на текущем Y)
            mergeNode.setLayoutX(currentPoint.x());
            mergeNode.setLayoutY(currentPoint.y());

            // Продвигаем Y-координату для следующего элемента в очереди
            double nextY = currentPoint.y() + Step.HEIGHT + Step.STEP;
            currentPoint = Point.of(currentPoint.x(),
                    nextY,
                    Step.WIDTH,
                    0,
                    nextY - Step.STEP);
        }
    }

    private static Point draw(final Step step, final Point p, Set<Step> visited, Deque<Step> waiting) {
        if (visited.contains(step)) {
            // Если узел посещен, возвращаем его положение и нулевую высоту
            return Point.of(step.getLayoutX(), step.getLayoutY(), Step.WIDTH, 0, step.getLayoutY() + Step.HEIGHT);
        }

        step.setLayoutX(p.x);
        step.setLayoutY(p.y);
        visited.add(step);

        List<Step> nextSteps = step.getNextSteps();

        if (nextSteps.isEmpty()) {
            // Конечный узел: возвращаем его высоту и maxY
            return Point.of(p.x, p.y, Step.WIDTH, Step.HEIGHT, p.y + Step.HEIGHT);
        }

        Step nextStep = nextSteps.getFirst();

        // --- 1. Обработка ветвления (Branch) ---
        if (step instanceof Branch && nextSteps.size() >= 2) {

            double negativeX = p.x + Step.WIDTH + Step.STEP;
            Point negStart = Point.of(negativeX, p.y + Step.HEIGHT + Step.STEP);
            Point negBranch = draw(nextSteps.get(1), negStart, visited, waiting);

            Point posStart = Point.of(p.x, p.y + Step.HEIGHT + Step.STEP);
            Point posBranch = draw(nextStep, posStart, visited, waiting);

            double totalHeight = Step.HEIGHT + Step.STEP + Math.max(posBranch.height, negBranch.height);
            double totalWidth = Step.WIDTH + Step.STEP + negBranch.width;

            // Максимальный Y - это Y начала + общая высота
            double maxY = p.y + totalHeight;

            return Point.of(p.x, p.y, totalWidth, totalHeight, maxY);
        }

        // --- 2. Обработка слияния (Merge Point) ---
        if (nextStep.getPreviousSteps().size() > 1) {
            if (!waiting.contains(nextStep)) {
                waiting.add(nextStep);
            }
            // Узел слияния откладывается, возвращаем высоту текущего узла и его maxY
            return Point.of(p.x, p.y, Step.WIDTH, Step.HEIGHT, p.y + Step.HEIGHT);
        }

        // --- 3. Обработка "Шампура" (Sequential Step) ---
        Point nextP = Point.of(p.x, p.y + Step.HEIGHT + Step.STEP);
        Point result = draw(nextStep, nextP, visited, waiting);

        // Объединяем текущий шаг с результатом рекурсии
        double totalHeight = Step.HEIGHT + Step.STEP + result.height;
        // Максимальный Y - это maxY, возвращенный рекурсией
        return Point.of(p.x, p.y, Step.WIDTH, totalHeight, result.maxY);
    }
}