package com.alensoft.automator42.model.line;

import javafx.geometry.Point2D;

public enum ArrowType {

    MAIN(0, 0.5, 1, 0, 0, 1),//down, left or right, down
    FROM_MAIN(0.5, 0, 1, 0, 0, 1), //right, right, down
    TO_MAIN(0, 1, 1, 0), //down left
    TO_MAIN_EMPTY(25, 0, 0, 1, 1, 0), //rigth absolute 25 px, down, left
    TO_MAIN_ABOVE(0, 25, 100, 0, 0, 1, 1, 0);//down 25 px, right 100 px, up, left

    final double[] dXY;

    ArrowType(double... dXY) {
        this.dXY = dXY;
    }

    public ArrowType resolve(Point2D start, Point2D end) {
        if (this == MAIN || this == FROM_MAIN || this == TO_MAIN_EMPTY) {
            return this;
        }
        if (start.getY() > end.getY()) {
            return TO_MAIN_ABOVE;
        } else { //down out
            return this;
        }
    }
}
