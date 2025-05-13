package at.primetshofer;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.Controller;
import at.primetshofer.model.TTS;
import at.primetshofer.model.util.DatabaseBackupManager;
import at.primetshofer.model.util.DiscordActivityUtil;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.MainMenuView;
import at.primetshofer.view.ViewUtils;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class Main extends Application {

    private static final Logger logger = Logger.getLogger(Main.class);

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

                DiscordActivityUtil.startDiscordConnection();

                initializeHibernate();

                DatabaseBackupManager.checkAndBackup(HibernateUtil.getEntityManager());

                return null;
            }
        };

        splashScreen.bindProgress(initializationTask);

        initializationTask.setOnSucceeded(e -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            ViewUtils.applyStyleSheet();
            TTS.updateSpeakerId();
            LangController.initBundle(Controller.getInstance().getSettings().getLocale());

            splashScreen.close();
            showMainStage(stage);
        });

        new Thread(initializationTask).start();
    }

    private void initializeHibernate() {
        try {
            HibernateUtil.getEntityManager();
        } catch (Exception ex) {
            logger.fatal("DB connection Failed. \nMake sure that no other instances of the program are running", ex);

            ViewUtils.showAlert(Alert.AlertType.ERROR,
                    LangController.getText("DBConnectionFailed"),
                    LangController.getText("ErrorText"));
            System.exit(100);
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

        ViewUtils.applyStyleSheetDefault();

        stage.setTitle("Japanese Learning App");
        stage.setScene(scene);
        stage.getIcons().add(new Image("icon.png"));
        stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AudioRecorder.stopRecording(null);
            HibernateUtil.shutdown();
            DiscordActivityUtil.stopDiscordConnection();
            logger.info("Shutdown Successful");
        }));

        primaryStage = stage;
    }
}