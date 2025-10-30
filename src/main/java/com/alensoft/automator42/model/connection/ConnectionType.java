package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.BaseNode;
import com.alensoft.automator42.model.line.ArrowType;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;

/**
 * Типы соединений для разных узлов.
 * Определяет точки подключения и метки.
 */
public enum ConnectionType {
    // MAIN - прямой поток (bottom → top) использует Arrow
    MAIN("", AnchorPoint.BOTTOM, AnchorPoint.TOP, ArrowType.MAIN),
    YES("Yes", AnchorPoint.BOTTOM, AnchorPoint.TOP, ArrowType.MAIN),
    NO("No", AnchorPoint.RIGHT, AnchorPoint.TOP, ArrowType.FROM_MAIN),
    MERGE("No", AnchorPoint.BOTTOM, AnchorPoint.TOP, ArrowType.TO_MAIN);

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

    public ObjectProperty<Point2D> getSourcePoint(BaseNode node) {
        return sourceAnchor.getPoint(node);
    }

    public ObjectProperty<Point2D> getTargetPoint(BaseNode node) {
        return targetAnchor.getPoint(node);
    }

    /**
     * Точки подключения на узле
     */
    public enum AnchorPoint {
        TOP {
            @Override
            public ObjectProperty<Point2D> getPoint(BaseNode node) {
                return node.getTop();
            }
        },
        BOTTOM {
            @Override
            public ObjectProperty<Point2D> getPoint(BaseNode node) {
                return node.getBottom();
            }
        },
        LEFT {
            @Override
            public ObjectProperty<Point2D> getPoint(BaseNode node) {
                return node.getLeft();
            }
        },
        RIGHT {
            @Override
            public ObjectProperty<Point2D> getPoint(BaseNode node) {
                return node.getRight();
            }
        };

        public abstract ObjectProperty<Point2D> getPoint(BaseNode node);
    }
}