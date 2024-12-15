package at.primetshofer;

import atlantafx.base.theme.CupertinoDark;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SplashScreen {

    private final Stage splashStage;
    private final ProgressBar progressBar;
    private final Label loadingLabel;

    public SplashScreen() {
        splashStage = new Stage(StageStyle.UNDECORATED);
        splashStage.getIcons().add(new Image("icon.png"));

        VBox splashLayout = new VBox(10);
        splashLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        loadingLabel = new Label("Loading...");
        loadingLabel.getStyleClass().add("smallText");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(25);

        splashLayout.getChildren().addAll(loadingLabel, progressBar);

        Scene splashScene = new Scene(splashLayout, 300, 200);
        splashScene.getStylesheets().add("myStyle.css");
        splashScene.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        splashStage.setScene(splashScene);
    }

    public void show() {
        splashStage.show();
    }

    public void close() {
        splashStage.close();
    }

    public void bindProgress(Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        loadingLabel.textProperty().bind(task.messageProperty());
    }
}
