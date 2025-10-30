module module_name {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.compiler;

    exports com.alensoft.automator42.model;
    exports com.alensoft.automator42.model.connection;

    opens com.alensoft.automator42.model to javafx.graphics;
    opens com.alensoft.automator42.model.line to javafx.graphics;
    opens com.alensoft.automator42.model.node to javafx.graphics;
    opens com.alensoft.automator42.model.connection to javafx.graphics;
}