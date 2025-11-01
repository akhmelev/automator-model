package com.alensoft.automator42.model;

import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.step.Branch;
import com.alensoft.automator42.model.step.Process;
import com.alensoft.automator42.model.step.Step;
import com.alensoft.automator42.model.step.UserIO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ComponentsDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(500, 20);
        Step begin = canvas.getSelectedStep();
        Step loadData = canvas.addStep(begin, new Process("Load Data"));
        Step decision = canvas.addStep(loadData, new Branch("OK?"));
        Step io = canvas.addStep(decision, new UserIO("User Input"));

        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        Button addProcessBtn = new Button("Добавить процесс");
        addProcessBtn.setOnAction(e -> {
            Process step = new Process("New Process");
            Step lastStep = canvas.addStep(canvas.getSelectedStep(), step);
            canvas.setSelectedStep(lastStep);
        });

        Button addDecisionBtn = new Button("Добавить решение");
        addDecisionBtn.setOnAction(e -> {
            Branch step = new Branch("Decision?");
            Step lastStep = canvas.addStep(canvas.getSelectedStep(), step);
            canvas.setSelectedStep(lastStep);
        });



        BorderPane topBar = new BorderPane();
        Pane ctrl = new Pane();
        ctrl.setPadding(new Insets(8));
        addProcessBtn.relocate(10, 5);
        addDecisionBtn.relocate(140, 5);
        ctrl.getChildren().addAll(addProcessBtn, addDecisionBtn);
        topBar.setCenter(ctrl);
        root.setTop(topBar);

        Scene scene = new Scene(root);
        primaryStage.setTitle("Flowchart components — JavaFX Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}