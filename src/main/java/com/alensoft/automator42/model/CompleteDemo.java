package com.alensoft.automator42.model;

import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.connection.ConnectionTool;
import com.alensoft.automator42.model.connection.ConnectionType;
import com.alensoft.automator42.model.node.Branch;
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
 * Полнофункциональная демонстрация системы с поддержкой вставки/удаления узлов
 */
public class CompleteDemo extends Application {

    private Canvas canvas;
    private ConnectionTool connectionTool;
    private Label statusLabel;
    private BaseNode selectedNode;
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

        updateStatus("Ready. Click on node to select, then use Insert/Delete buttons");
    }

    private void buildDemoFlowchart() {
        BaseNode begin = canvas.getSelectedNode();

        // Основная цепочка с Decision (автоматически создаются коннекторы)
        BaseNode init = canvas.addMainNode(begin, new ProcessNode("Initialize"));
        BaseNode validation = canvas.addBranch(init, new Branch("Config Valid?"));

        // Активировать выделение и ConnectionTool для всех узлов (включая Connector)
        canvas.getChildren().stream()
                .filter(node -> node instanceof BaseNode)
                .map(node -> (BaseNode) node)
                .forEach(this::activateNode);

        updateStatus("Demo flowchart created. All branches lead to End. AST is valid.");
    }

    private void activateNode(BaseNode node) {
        // Добавить обработчик клика для выделения
        node.setOnMouseClicked(e -> {
            if (!e.isControlDown()) {
                selectNode(node);
                e.consume();
            }
        });

        // Активировать ConnectionTool (Ctrl+Drag для связей)
        connectionTool.activate(node);
    }

    private void selectNode(BaseNode node) {
        // Снять выделение с предыдущего
        if (selectedNode != null) {
            selectedNode.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
        }

        // Выделить новый узел
        selectedNode = node;
        selectedNode.setStyle("-fx-border-color: #49fc4e; -fx-border-width: 3; -fx-border-radius: 5;");
        updateStatus("Selected: " + node.getText() + " (" + node.getClass().getSimpleName() + ")");
    }

    private VBox createToolbar() {
        VBox toolbar = new VBox(5);
        toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        // Панель добавления узлов
        toolbar.getChildren().add(createAddNodesPanel());
        toolbar.getChildren().add(new Separator());

        // Панель вставки/удаления
        toolbar.getChildren().add(createEditPanel());
        toolbar.getChildren().add(new Separator());

        // Панель управления связями
        toolbar.getChildren().add(createConnectionPanel());

        // Инструкция
        Label instruction = new Label(
                "💡 Instructions:\n" +
                "• Click node to select (orange border)\n" +
                "• Use Insert buttons to add nodes before/after selected\n" +
                "• Hold Ctrl + Drag to create connections\n" +
                "• Delete removes node and reconnects neighbors"
        );
        instruction.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        instruction.setPadding(new Insets(5, 10, 5, 10));
        instruction.setWrapText(true);
        toolbar.getChildren().add(instruction);

        return toolbar;
    }

    private HBox createAddNodesPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER_LEFT);

        Button addProcessBtn = new Button("➕ Process");
        addProcessBtn.setOnAction(e -> addNodeAtEnd(new ProcessNode("New Process")));

        Button addDecisionBtn = new Button("➕ Decision");
        addDecisionBtn.setOnAction(e -> addNodeAtEnd(new Branch("Decision?")));

        Button addIOBtn = new Button("➕ I/O");
        addIOBtn.setOnAction(e -> addNodeAtEnd(new UserIO("Input/Output")));

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
        insertProcessBtn.setOnAction(e -> insertNode(new ProcessNode("Inserted Process")));

        Button insertDecisionBtn = new Button("⬇ Insert Decision");
        insertDecisionBtn.setOnAction(e -> insertNode(new Branch("Inserted?")));

        Button insertIOBtn = new Button("⬇ Insert I/O");
        insertIOBtn.setOnAction(e -> insertNode(new UserIO("Inserted I/O")));

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

        Button clearConnectionsBtn = new Button("🗑 Clear All Connections");
        clearConnectionsBtn.setOnAction(e -> {
            canvas.clearConnections();
            updateStatus("All connections cleared");
        });

        Button validateBtn = new Button("✓ Validate AST");
        validateBtn.setOnAction(e -> {
            boolean valid = canvas.validateAST();
            updateStatus("AST validation: " + (valid ? "✓ Valid" : "✗ Invalid - disconnected nodes exist"));
        });

        panel.getChildren().addAll(
                new Label("Connection type:"),
                connectionTypeBox,
                clearConnectionsBtn,
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

    private void addNodeAtEnd(BaseNode node) {
        BaseNode lastNode = canvas.getSelectedNode();
        if (lastNode == null) {
            updateStatus("Error: No last node found");
            return;
        }

        BaseNode added;
        if (node instanceof Branch) {
            added = canvas.addBranch(lastNode, node);
        } else {
            added = canvas.addMainNode(lastNode, node);
        }

        canvas.setSelectedNode(added);
        activateNode(added);
        updateStatus("Added " + node.getClass().getSimpleName() + " at end");
    }

    private void insertNode(BaseNode node) {
        if (selectedNode == null) {
            updateStatus("Error: No node selected. Click a node first.");
            return;
        }

        try {
            String mode = insertModeBox.getValue();
            BaseNode inserted = null;

            switch (mode) {
                case "After (Main)":
                    if (node instanceof Branch) {
                        inserted = canvas.addBranch(selectedNode, node);
                    } else {
                        inserted = canvas.addMainNode(selectedNode, node);
                    }
                    break;

                case "After (NO)":
                    if (!(selectedNode instanceof Branch)) {
                        updateStatus("Error: Selected node must be Decision for NO branch");
                        return;
                    }
                    inserted = canvas.insertToBranch(selectedNode, node);
                    break;

                case "Before":
                    // Найти предшественника выбранного узла
                    BaseNode predecessor = findPredecessor(selectedNode);
                    if (predecessor == null) {
                        updateStatus("Error: Cannot insert before root node");
                        return;
                    }
                    if (node instanceof Branch) {
                        inserted = canvas.addBranch(predecessor, node);
                    } else {
                        inserted = canvas.addMainNode(predecessor, node);
                    }
                    break;
            }

            if (inserted != null) {
                activateNode(inserted);
                selectNode(inserted);
                updateStatus("Inserted " + node.getClass().getSimpleName() + " " + mode);
            }

        } catch (Exception e) {
            updateStatus("Error: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        if (selectedNode == null) {
            updateStatus("Error: No node selected");
            return;
        }

        String nodeName = selectedNode.getText();
        canvas.removeNode(selectedNode);
        selectedNode = null;

        updateStatus("Deleted: " + nodeName);
    }

    private BaseNode findPredecessor(BaseNode node) {
        return canvas.getChildren().stream()
                .filter(n -> n instanceof BaseNode)
                .map(n -> (BaseNode) n)
                .filter(n -> canvas.getConnectionManager()
                        .getOutgoingConnections(n)
                        .stream()
                        .anyMatch(conn -> conn.getTarget() == node))
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