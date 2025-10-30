package com.alensoft.automator42.model;

import com.alensoft.automator42.model.canvas.Canvas;
import com.alensoft.automator42.model.node.Decision;
import com.alensoft.automator42.model.node.ProcessNode;
import com.alensoft.automator42.model.node.UserIO;
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
        BaseNode begin = canvas.getLastNode();
        BaseNode loadData = canvas.addMainNode(begin, new ProcessNode("Load Data"));
        BaseNode decision = canvas.addMainNode(loadData, new Decision("OK?"));
        BaseNode io = canvas.addMainNode(decision, new UserIO("User Input"));

        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        Button addProcessBtn = new Button("Добавить процесс");
        addProcessBtn.setOnAction(e -> {
            ProcessNode node = new ProcessNode("New Process");
            BaseNode lastNode = canvas.addMainNode(canvas.getLastNode(), node);
            canvas.setLastNode(lastNode);
        });

        Button addDecisionBtn = new Button("Добавить решение");
        addDecisionBtn.setOnAction(e -> {
            Decision node = new Decision("Decision?");
            BaseNode lastNode = canvas.addDecisionNode(canvas.getLastNode(), node);
            canvas.setLastNode(lastNode);
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