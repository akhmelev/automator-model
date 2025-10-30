package com.alensoft.automator42.model;

import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.connection.ConnectionTool;
import com.alensoft.automator42.model.connection.ConnectionType;
import com.alensoft.automator42.model.node.Decision;
import com.alensoft.automator42.model.node.ProcessNode;
import com.alensoft.automator42.model.node.UserIO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Полнофункциональная демонстрация системы связей с множественными выходами
 */
public class CompleteDemo extends Application {

    private Canvas canvas;
    private ConnectionTool connectionTool;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(400, 25);
        connectionTool = new ConnectionTool(canvas, canvas.getConnectionManager());

        // Построение демо-схемы
        buildDemoFlowchart();

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setTop(createToolbar());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setTitle("Flowchart Editor - Multiple Connections Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void buildDemoFlowchart() {
        BaseNode begin = canvas.getLastNode();
        
        // Основная цепочка - используем MAIN (прямые стрелки)
        BaseNode init = canvas.addMainNode(begin, new ProcessNode("Initialize"));
        BaseNode loadConfig = canvas.addMainNode(init, new ProcessNode("Load Config"));
        BaseNode validation = canvas.addMainNode(loadConfig, new Decision("Config Valid?"));
  /*
        // Ветка "Yes" - успешная валидация (ArrowIf от right)
        ProcessNode connectDB = new ProcessNode("Connect to DB");
        connectDB.relocate(validation.getLayoutX() + 250, validation.getLayoutY() + 120);
        canvas.addI(connectDB, connectDB.getLayoutX(), connectDB.getLayoutY());
        connectionTool.activate(connectDB);
        
        ProcessNode loadData = new ProcessNode("Load Data");
        loadData.relocate(connectDB.getLayoutX(), connectDB.getLayoutY() + 100);
        canvas.addNode(loadData, loadData.getLayoutX(), loadData.getLayoutY());
        connectionTool.activate(loadData);
        
        // Ветка "No" - ошибка валидации (ArrowIf от left)
        UserIO showError = new UserIO("Show Error");
        showError.relocate(validation.getLayoutX() - 250, validation.getLayoutY() + 120);
        canvas.addNode(showError, showError.getLayoutX(), showError.getLayoutY());
        connectionTool.activate(showError);
        
        ProcessNode useDefaults = new ProcessNode("Use Defaults");
        useDefaults.relocate(showError.getLayoutX(), showError.getLayoutY() + 100);
        canvas.addNode(useDefaults, useDefaults.getLayoutX(), useDefaults.getLayoutY());
        connectionTool.activate(useDefaults);
        
        // Создание связей - YES/NO используют ArrowIf
        canvas.getConnectionManager().createConnection(validation, connectDB, ConnectionType.NO);
        canvas.getConnectionManager().createConnection(validation, showError, ConnectionType.YES);
        
        // Прямые стрелки внутри веток - используем MAIN (Arrow)
        canvas.getConnectionManager().createConnection(connectDB, loadData, ConnectionType.MAIN);
        canvas.getConnectionManager().createConnection(showError, useDefaults, ConnectionType.MAIN);
        
        // Слияние веток
        Decision dataCheck = new Decision("Data OK?");
        dataCheck.relocate(validation.getLayoutX(), validation.getLayoutY() + 250);
        canvas.addNode(dataCheck, dataCheck.getLayoutX(), dataCheck.getLayoutY());
        connectionTool.activate(dataCheck);
        
        // Слияние - используем ArrowIf для боковых входов
        canvas.getConnectionManager().createConnection(loadData, dataCheck, ConnectionType.MAIN);
        canvas.getConnectionManager().createConnection(useDefaults, dataCheck, ConnectionType.MAIN);
        
        // Финальная обработка
        ProcessNode processData = new ProcessNode("Process Data");
        processData.relocate(dataCheck.getLayoutX() + 200, dataCheck.getLayoutY() + 100);
        canvas.addNode(processData, processData.getLayoutX(), processData.getLayoutY());
        connectionTool.activate(processData);
        
        UserIO retry = new UserIO("Retry?");
        retry.relocate(dataCheck.getLayoutX() - 200, dataCheck.getLayoutY() + 100);
        canvas.addNode(retry, retry.getLayoutX(), retry.getLayoutY());
        connectionTool.activate(retry);
        
        // Разветвление от второго Decision - YES/NO используют ArrowIf
        canvas.getConnectionManager().createConnection(dataCheck, processData, ConnectionType.NO);
        canvas.getConnectionManager().createConnection(dataCheck, retry, ConnectionType.YES);
        */
        // Активировать инструмент для всех узлов основной цепочки
        connectionTool.activate(begin);
        connectionTool.activate(init);
        connectionTool.activate(loadConfig);
        connectionTool.activate(validation);
    }

    private VBox createToolbar() {
        // Панель добавления узлов
        HBox addNodesPanel = new HBox(10);
        addNodesPanel.setPadding(new Insets(10));
        addNodesPanel.setAlignment(Pos.CENTER_LEFT);
        
        Button addProcessBtn = new Button("➕ Process");
        addProcessBtn.setOnAction(e -> addNode(new ProcessNode("Process")));
        
        Button addDecisionBtn = new Button("➕ Decision");
        addDecisionBtn.setOnAction(e -> addNode(new Decision("Decision?")));
        
        Button addIOBtn = new Button("➕ I/O");
        addIOBtn.setOnAction(e -> addNode(new UserIO("Input/Output")));
        
        addNodesPanel.getChildren().addAll(
            new Label("Add:"), addProcessBtn, addDecisionBtn, addIOBtn
        );
        
        // Панель управления
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        
        Button clearConnectionsBtn = new Button("🗑 Clear Connections");
        clearConnectionsBtn.setOnAction(e -> {
            canvas.clearConnections();
            updateStatus("All connections cleared");
        });
        
        ComboBox<ConnectionType> connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.MAIN);
        connectionTypeBox.setOnAction(e -> {
            connectionTool.setDefaultConnectionType(connectionTypeBox.getValue());
            updateStatus("Default connection type: " + connectionTypeBox.getValue());
        });
        
        controlPanel.getChildren().addAll(
            new Label("Tools:"), clearConnectionsBtn,
            new Separator(),
            new Label("Connection Type:"), connectionTypeBox
        );
        
        // Инструкция
        Label instruction = new Label(
            "💡 Instructions: Hold Ctrl and drag from one node to another to create a connection. " +
            "Click on left/right side of Decision nodes for Yes/No branches."
        );
        instruction.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        instruction.setPadding(new Insets(5, 10, 5, 10));
        instruction.setWrapText(true);
        
        VBox toolbar = new VBox(5);
        toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        toolbar.getChildren().addAll(addNodesPanel, new Separator(), controlPanel, instruction);
        
        return toolbar;
    }

    private HBox createStatusBar() {
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 11px;");
        
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");
        
        return statusBar;
    }

    private void addNode(BaseNode node) {
//        double x = 100 + Math.random() * 600;
//        double y = 100 + Math.random() * 400;
//        canvas.addNode(node, x, y);
//        connectionTool.activate(node);
//        updateStatus("Added " + node.getClass().getSimpleName() + " at (" + (int)x + ", " + (int)y + ")");
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}