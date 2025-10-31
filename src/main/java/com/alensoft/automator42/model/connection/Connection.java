package com.alensoft.automator42.model.connection;

import com.alensoft.automator42.model.step.Step;
import javafx.scene.Group;

/**
 * Представляет соединение между двумя узлами.
 * Отделяет логику связей от узлов (SRP).
 */
public class Connection {
    private final Step source;
    private final Step target;
    private final ConnectionType type;
    private final Group arrow;

    public Connection(Step source, Step target, ConnectionType type, Group arrow) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target steps cannot be null");
        }
        this.source = source;
        this.target = target;
        this.type = type;
        this.arrow = arrow;
    }

    public Step getSource() {
        return source;
    }

    public Step getTarget() {
        return target;
    }

    public ConnectionType getType() {
        return type;
    }

    public Group getArrow() {
        return arrow;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Connection other)) return false;
        return source.equals(other.source) && 
               target.equals(other.target) && 
               type == other.type;
    }

    @Override
    public int hashCode() {
        return source.hashCode() * 31 + target.hashCode() * 17 + type.hashCode();
    }
}