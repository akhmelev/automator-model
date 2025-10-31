package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.step.Step;
import com.alensoft.automator42.model.line.ArrowType;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;

/**
 * Типы соединений для разных узлов.
 * Определяет точки подключения и метки.
 */
public enum ConnectionType {
    // MAIN - прямой поток (bottom → top) использует Arrow
    DOWN("", AnchorPoint.BOTTOM, AnchorPoint.TOP, ArrowType.OK),
    BRANCH("No", AnchorPoint.RIGHT, AnchorPoint.TOP, ArrowType.IN),
    EMPTY("No", AnchorPoint.RIGHT, AnchorPoint.TOP, ArrowType.IN_EMPTY),
    MERGE("No", AnchorPoint.BOTTOM, AnchorPoint.TOP, ArrowType.OUT);

    private final String label;
    private final AnchorPoint sourceAnchor;
    private final AnchorPoint targetAnchor;

    public ArrowType getDirection() {
        return direction;
    }

    private final ArrowType direction; // true = ArrowIf, false = Arrow

    ConnectionType(String label, AnchorPoint sourceAnchor, AnchorPoint targetAnchor, ArrowType direction) {
        this.label = label;
        this.sourceAnchor = sourceAnchor;
        this.targetAnchor = targetAnchor;
        this.direction = direction;
    }

    public String getLabel() {
        return label;
    }

    public ObjectProperty<Point2D> getSourcePoint(Step step) {
        return sourceAnchor.getPoint(step);
    }

    public ObjectProperty<Point2D> getTargetPoint(Step step) {
        return targetAnchor.getPoint(step);
    }

    /**
     * Точки подключения на узле
     */
    public enum AnchorPoint {
        TOP {
            @Override
            public ObjectProperty<Point2D> getPoint(Step step) {
                return step.getTop();
            }
        },
        BOTTOM {
            @Override
            public ObjectProperty<Point2D> getPoint(Step step) {
                return step.getBottom();
            }
        },
        LEFT {
            @Override
            public ObjectProperty<Point2D> getPoint(Step step) {
                return step.getLeft();
            }
        },
        RIGHT {
            @Override
            public ObjectProperty<Point2D> getPoint(Step step) {
                return step.getRight();
            }
        };

        public abstract ObjectProperty<Point2D> getPoint(Step step);
    }
}