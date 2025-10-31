package com.alensoft.automator42.model;

import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.connection.ConnectionTool;
import com.alensoft.automator42.model.connection.ConnectionType;
import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Process;
import com.alensoft.automator42.model.step.Step;
import com.alensoft.automator42.model.step.UserIO;
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
 * Полнофункциональная демонстрация системы с поддержкой вставки/удаления узлов
 */
public class CompleteDemo extends Application {

    private Canvas canvas;
    private ConnectionTool connectionTool;
    private Label statusLabel;
    private Step selectedStep;
    private ComboBox<String> insertModeBox;

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(400, 25);
        connectionTool = new ConnectionTool(canvas, canvas.getConnectionManager());

        // Построение демо-схемы

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setTop(createToolbar());
        root.setBottom(createStatusBar());

        buildDemoFlowchart();
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setTitle("DRAKON Flowchart Editor - AST Preserving Demo");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateStatus("Ready. Click on step to select, then use Insert/Delete buttons");
    }

    private void buildDemoFlowchart() {
        Step begin = canvas.getSelectedStep();

        // Основная цепочка с Decision (автоматически создаются коннекторы)
        Step init = canvas.addStep(begin, new Process("Initialize"));
        Step validation = canvas.addBranch(init, new Branch("Config Valid?"));

        // Активировать выделение и ConnectionTool для всех узлов (включая Connector)
        canvas.getChildren().stream()
                .filter(step -> step instanceof Step)
                .map(step -> (Step) step)
                .forEach(this::activateStep);

        updateStatus("Demo flowchart created. All branches lead to End. AST is valid.");
    }

    private void activateStep(Step step) {
        // Добавить обработчик клика для выделения
        step.setOnMouseClicked(e -> {
            if (!e.isControlDown()) {
                selectStep(step);
                e.consume();
            }
        });

        // Активировать ConnectionTool (Ctrl+Drag для связей)
        connectionTool.activate(step);
    }

    private void selectStep(Step step) {
        // Снять выделение с предыдущего
        if (selectedStep != null) {
            selectedStep.setStyle("-fx-border-color: transparent; -fx-border-width: 8;");
        }

        // Выделить новый узел
        selectedStep = step;
        selectedStep.setStyle("-fx-border-color: #49fc4e; -fx-border-width: 3; -fx-border-radius: 5;");
        updateStatus("Selected: " + step.getText() + " (" + step.getClass().getSimpleName() + ")");
    }

    private VBox createToolbar() {
        VBox toolbar = new VBox(5);
        toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        // Панель добавления узлов
        toolbar.getChildren().add(createAddStepsPanel());
        toolbar.getChildren().add(new Separator());

        // Панель вставки/удаления
        toolbar.getChildren().add(createEditPanel());
        toolbar.getChildren().add(new Separator());

        // Панель управления связями
        toolbar.getChildren().add(createConnectionPanel());

        // Инструкция
        Label instruction = new Label(
                "💡 Instructions:\n" +
                "• Click step to select (orange border)\n" +
                "• Use Insert buttons to add steps before/after selected\n" +
                "• Hold Ctrl + Drag to create connections\n" +
                "• Delete removes step and reconnects neighbors"
        );
        instruction.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        instruction.setPadding(new Insets(5, 10, 5, 10));
        instruction.setWrapText(true);
        toolbar.getChildren().add(instruction);

        return toolbar;
    }

    private HBox createAddStepsPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER_LEFT);

        Button addProcessBtn = new Button("➕ Process");
        addProcessBtn.setOnAction(e -> addStepAtEnd(new Process("New Process")));

        Button addDecisionBtn = new Button("➕ Decision");
        addDecisionBtn.setOnAction(e -> addStepAtEnd(new Branch("Decision?")));

        Button addIOBtn = new Button("➕ I/O");
        addIOBtn.setOnAction(e -> addStepAtEnd(new UserIO("Input/Output")));

        panel.getChildren().addAll(
                new Label("Add to end:"),
                addProcessBtn,
                addDecisionBtn,
                addIOBtn
        );

        return panel;
    }

    private HBox createEditPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER_LEFT);

        // Выбор режима вставки
        insertModeBox = new ComboBox<>();
        insertModeBox.getItems().addAll("After (Main)", "After (YES)", "After (NO)", "Before");
        insertModeBox.setValue("After (Main)");
        insertModeBox.setPrefWidth(130);

        Button insertProcessBtn = new Button("⬇ Insert Process");
        insertProcessBtn.setOnAction(e -> insertStep(new Process("Inserted Process")));

        Button insertDecisionBtn = new Button("⬇ Insert Decision");
        insertDecisionBtn.setOnAction(e -> insertStep(new Branch("Inserted?")));

        Button insertIOBtn = new Button("⬇ Insert I/O");
        insertIOBtn.setOnAction(e -> insertStep(new UserIO("Inserted I/O")));

        Button deleteBtn = new Button("🗑 Delete Selected");
        deleteBtn.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828;");
        deleteBtn.setOnAction(e -> deleteSelected());

        panel.getChildren().addAll(
                new Label("Insert mode:"),
                insertModeBox,
                insertProcessBtn,
                insertDecisionBtn,
                insertIOBtn,
                new Separator(),
                deleteBtn
        );

        return panel;
    }

    private HBox createConnectionPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER_LEFT);

        ComboBox<ConnectionType> connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.DOWN);
        connectionTypeBox.setOnAction(e -> {
            connectionTool.setDefaultConnectionType(connectionTypeBox.getValue());
            updateStatus("Connection mode: " + connectionTypeBox.getValue());
        });



        Button validateBtn = new Button("✓ Validate AST");
        validateBtn.setOnAction(e -> {
            boolean valid = canvas.validateAST();
            updateStatus("AST validation: " + (valid ? "✓ Valid" : "✗ Invalid - disconnected steps exist"));
        });

        panel.getChildren().addAll(
                new Label("Connection type:"),
                connectionTypeBox,
                validateBtn
        );

        return panel;
    }

    private HBox createStatusBar() {
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 11px;");

        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        return statusBar;
    }

    // ============= ОПЕРАЦИИ С УЗЛАМИ =============

    private void addStepAtEnd(Step step) {
        Step lastStep = canvas.getSelectedStep();
        if (lastStep == null) {
            updateStatus("Error: No last step found");
            return;
        }

        Step added;
        if (step instanceof Branch) {
            added = canvas.addBranch(lastStep, step);
        } else {
            added = canvas.addStep(lastStep, step);
        }

        canvas.setSelectedStep(added);
        activateStep(added);
        updateStatus("Added " + step.getClass().getSimpleName() + " at end");
    }

    private void insertStep(Step step) {
        if (selectedStep == null) {
            updateStatus("Error: No step selected. Click a step first.");
            return;
        }

        try {
            String mode = insertModeBox.getValue();
            Step inserted = null;

            switch (mode) {
                case "After (Main)":
                    if (step instanceof Branch) {
                        inserted = canvas.addBranch(selectedStep, step);
                    } else {
                        inserted = canvas.addStep(selectedStep, step);
                    }
                    break;

                case "After (NO)":
                    if (!(selectedStep instanceof Branch)) {
                        updateStatus("Error: Selected step must be Decision for NO branch");
                        return;
                    }
                    inserted = canvas.insertInBranch(selectedStep, step);
                    break;

                case "Before":
                    // Найти предшественника выбранного узла
                    Step predecessor = findPredecessor(selectedStep);
                    if (predecessor == null) {
                        updateStatus("Error: Cannot insert before root step");
                        return;
                    }
                    if (step instanceof Branch) {
                        inserted = canvas.addBranch(predecessor, step);
                    } else {
                        inserted = canvas.addStep(predecessor, step);
                    }
                    break;
            }

            if (inserted != null) {
                activateStep(inserted);
                selectStep(inserted);
                updateStatus("Inserted " + step.getClass().getSimpleName() + " " + mode);
            }

        } catch (Exception e) {
            updateStatus("Error: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        if (selectedStep == null) {
            updateStatus("Error: No step selected");
            return;
        }

        String stepName = selectedStep.getText();
        canvas.removeStep(selectedStep);
        selectedStep = null;

        updateStatus("Deleted: " + stepName);
    }

    private Step findPredecessor(Step step) {
        return canvas.getChildren().stream()
                .filter(n -> n instanceof Step)
                .map(n -> (Step) n)
                .filter(n -> n.out()
                        .stream()
                        .anyMatch(conn -> conn.getTarget() == step))
                .findFirst()
                .orElse(null);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("[Status] " + message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}