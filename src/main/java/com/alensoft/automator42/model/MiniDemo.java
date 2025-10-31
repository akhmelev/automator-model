package com.alensoft.automator42.model;

import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.connection.ConnectionManager;
import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Process;
import com.alensoft.automator42.model.step.Step;
import com.alensoft.automator42.model.step.UserIO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MiniDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(500, 20);
        ConnectionManager cm = canvas.getConnectionManager();

        // Построение блок-схемы с множественными выходами
        Step begin = canvas.getSelectedStep();

//        // Основная цепочка - используем addMainStep (создает MAIN соединения с Arrow)
//        BaseStep loadData = canvas.addMainStep(begin, new ProcessStep("Load Data"));
//        BaseStep decision = canvas.addMainStep(loadData, new Decision("Data Valid?"));
//
//        // Создание веток для Decision - используем YES/NO (ArrowIf)
//        BaseStep processData = canvas.addMainStep(decision,new ProcessStep("Process Data"));
//
//        UserIO errorHandler = new UserIO("Show Error");
//        errorHandler.relocate(decision.getLayoutX() - 200, decision.getLayoutY() + 150);
//        canvas.getChildren().add(errorHandler);
//
//        // Множественные выходы от Decision - используем ArrowIf
//        cm.createConnection(decision, errorHandler, ConnectionType.NOK);
//
//        // Продолжение основной ветки
//        BaseStep saveResult = canvas.addMainStep(processData,new ProcessStep("Save Result"));
//        saveResult.relocate(decision.getLayoutX(), decision.getLayoutY() + 300);
//
//        // Слияние веток - используем MAIN (Arrow) так как идет вертикально вниз
//        cm.createConnection(processData, saveResult, ConnectionType.MAIN);
//        cm.createConnection(errorHandler, saveResult, ConnectionType.MERGE);

        // UI
        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        HBox toolbar = createToolbar(canvas);
        root.setTop(toolbar);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Flowchart with Multiple Connections");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private HBox createToolbar(Canvas canvas) {
        Button addProcessBtn = new Button("Add Process");
        addProcessBtn.setOnAction(e -> {
            Process step = new Process("New Process");
            Step lastStep = canvas.addStep(canvas.getSelectedStep(), step);
            canvas.setSelectedStep(lastStep);
        });

        Button addDecisionBtn = new Button("Add Decision");
        addDecisionBtn.setOnAction(e -> {
            Branch step = new Branch("Decision?");
            Step lastStep = canvas.addBranch(canvas.getSelectedStep(), step);
            canvas.setSelectedStep(lastStep);
        });

        Button addIOBtn = new Button("Add I/O");
        addIOBtn.setOnAction(e -> {
            UserIO step = new UserIO("User Input");
            Step lastStep = canvas.addStep(canvas.getSelectedStep(), step);
            canvas.setSelectedStep(lastStep);
        });

        Button clearConnectionsBtn = new Button("Clear Connections");
//        clearConnectionsBtn.setOnAction(e -> canvas.clearConnections());

        HBox toolbar = new HBox(10, addProcessBtn, addDecisionBtn, addIOBtn, clearConnectionsBtn);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");

        return toolbar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}