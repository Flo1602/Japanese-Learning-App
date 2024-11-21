package at.primetshofer;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.TTS;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.MainMenuView;
import at.primetshofer.view.ViewUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {

    public static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        try {
            HibernateUtil.getEntityManager();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("DB connection Failed! \nMake sure that no other instances of the program are running!");
            alert.showAndWait();
            System.exit(100);
        }

        Pane pane = new Pane();

        Scene scene = new Scene(pane, 1280, 720);
        scene.getStylesheets().add("myStyle.css");

        MainMenuView mainMenu = new MainMenuView(scene);
        mainMenu.display(null);

        ViewUtils.applyStyleSheet();
        TTS.updateSpeakerId();

        stage.setTitle("Japanese Learning App");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> AudioRecorder.stopRecording(null));

        primaryStage = stage;
    }
}