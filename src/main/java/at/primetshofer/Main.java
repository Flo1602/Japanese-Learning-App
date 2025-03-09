package at.primetshofer;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.TTS;
import at.primetshofer.model.util.DatabaseBackupManager;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.MainMenuView;
import at.primetshofer.view.ViewUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {

    public static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        SplashScreen splashScreen = new SplashScreen();

        splashScreen.show();

        Task<Void> initializationTask = new Task<>() {
            @Override
            protected Void call() {
                updateProgress(-1, 0);
                updateMessage(LangController.getText("SplashScreenMessage"));

                initializeHibernate();

                DatabaseBackupManager.checkAndBackup(HibernateUtil.getEntityManager());

                return null;
            }
        };

        splashScreen.bindProgress(initializationTask);

        initializationTask.setOnSucceeded(e -> {
            splashScreen.close();
            showMainStage(stage);
        });

        new Thread(initializationTask).start();
    }

    private void initializeHibernate() {
        try {
            HibernateUtil.getEntityManager();
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("DB connection Failed! \nMake sure that no other instances of the program are running!");
                alert.showAndWait();
                System.exit(100);
            });
        }
    }

    private void showMainStage(Stage stage) {
        Pane pane = new Pane();

        Scene scene = new Scene(pane, 1280, 720);
        scene.getStylesheets().add("myStyle.css");

        MainMenuView mainMenu = new MainMenuView(scene);
        mainMenu.display(null);

        ViewUtils.applyStyleSheet();
        TTS.updateSpeakerId();

        new Thread(() -> {

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            ViewUtils.applyStyleSheet();
            TTS.updateSpeakerId();
        }).start();

        ViewUtils.applyStyleSheetDefault();

        stage.setTitle("Japanese Learning App");
        stage.setScene(scene);
        stage.getIcons().add(new Image("icon.png"));
        stage.show();

        stage.setOnCloseRequest(event -> AudioRecorder.stopRecording(null));

        primaryStage = stage;
    }
}