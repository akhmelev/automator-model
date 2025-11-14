module module_name {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.compiler;

    exports com.alensoft.automator42.model;
    exports com.alensoft.automator42.model.connection;
    exports com.alensoft.automator42.model.line;
    exports com.alensoft.automator42.model.canvas;

    opens com.alensoft.automator42.model to javafx.graphics;
    opens com.alensoft.automator42.model.line to javafx.graphics;
    opens com.alensoft.automator42.model.step to javafx.graphics;
    opens com.alensoft.automator42.model.connection to javafx.graphics;
    exports com.alensoft.automator42.model.step;
}