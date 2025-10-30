package com.alensoft.automator42.model.line;

import javafx.geometry.Point2D;

public enum ArrowType {

    OK(0, 0.5, 1, 0, 0, 1),//down, left or right, down
    IN(0.5, 0, 1, 0, 0, 1), //right, right, down
    IN_EMPTY(25, 0, 0, 1, 1, 0), //rigth absolute 25 px, down, left

    OUT(0, 1, 1, 0), //down left
    OUT_TOP(0, 25, 100, 0,
            0, 1, 1, 0);//down 25 px, right 100 px, up, left

    final double[] dXY;

    ArrowType(double... dXY) {
        this.dXY = dXY;
    }

    public ArrowType update(Point2D start, Point2D end) {
        if (this == OK || this == IN || this == IN_EMPTY) {
            return this;
        }
        if (start.getY() > end.getY()) {
            return OUT_TOP;
        } else { //down out
            return OUT;
        }
    }
}
