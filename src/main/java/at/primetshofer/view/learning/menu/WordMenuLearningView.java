package at.primetshofer.view.learning.menu;

import at.primetshofer.model.Controller;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.CreateEditWordWindow;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnSessionManagers.WordDefenseSessionManager;
import at.primetshofer.view.learning.learnSessionManagers.WordSessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.log4j.Logger;

public class WordMenuLearningView extends View {

    private static final Logger logger = Logger.getLogger(WordMenuLearningView.class);

    private final Controller controller;
    private ProgressBar wordProgress;
    private Label wordProgressLabel;
    private Button addWordToDue;

    private WordSessionManager wordSessionManager;

    public WordMenuLearningView(Scene scene) {
        super(scene);
        controller = Controller.getInstance();
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("VocabButton"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        Button dailyWordsButton = new Button(LangController.getText("DailyWordsButton"));
        dailyWordsButton.getStyleClass().add("menuButton");
        dailyWordsButton.setOnAction(e -> {
            if (wordSessionManager == null) {
                wordSessionManager = new WordSessionManager(scene);
            }

            wordSessionManager.initSessionManager();
            wordSessionManager.initView();
            wordSessionManager.display(this);
        });

        Button speakingButton = new Button(LangController.getText("WordDefense"));
        speakingButton.getStyleClass().add("menuButton");
        speakingButton.setOnAction(e -> {
            WordDefenseSessionManager wordDefenseSessionManager = new WordDefenseSessionManager(scene);
            wordDefenseSessionManager.initSessionManager();
            wordDefenseSessionManager.initView();
            wordDefenseSessionManager.display(this);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(dailyWordsButton, speakingButton);
        BorderPane.setAlignment(vb, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        HBox bottom = new HBox();
        bottom.setSpacing(20);
        bottom.setAlignment(Pos.CENTER);

        addWordToDue = new Button("+");
        addWordToDue.getStyleClass().add("plusButton");

        addWordToDue.setOnAction(e -> addWordToDueAction());

        wordProgressLabel = new Label(LangController.getText("WordProgressLabel"));
        wordProgressLabel.getStyleClass().add("smallText");

        wordProgress = new ProgressBar();
        wordProgress.setProgress(0);
        wordProgress.setPrefSize(200, 25);

        bottom.getChildren().addAll(addWordToDue, wordProgressLabel, wordProgress);

        Image mobileImage = new Image("tablet.png");

        ImageView mobileImageView = new ImageView(mobileImage);
        mobileImageView.setFitHeight(50);
        mobileImageView.setFitWidth(50);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(vb);
        bp.setBottom(bottom);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private void addWordToDueAction() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                controller.increaseDueWordsTmp(1);
                controller.updateWordList();
                return null;
            }
        };

        task.setOnSucceeded(event -> updateProgress());
        task.setOnFailed(event -> {
            logger.fatal("Error while updating progresses", event.getSource().getException());
            // TODO: use lang
            ViewUtils.showAlert(Alert.AlertType.ERROR, "Error while updating Progresses!", "FATAL ERROR!");
        });

        new Thread(task).start();
    }

    @Override
    public void display(View origin) {
        super.display(origin);

        updateProgress();
    }

    private void updateProgress() {
        wordProgressLabel.setText(controller.getDueWordCount() + " " + LangController.getText("WordProgressLabel") + " (" + controller.getDueTotalWordsCount() + " " + LangController.getText("TotalLabel") + "):");

        setProgress(controller.getWordsProgress());

        addWordToDue.setVisible(controller.getDueWordCount() != controller.getDueTotalWordsCount());
    }

    public void setProgress(double progress) {
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.ZERO, // Start at 0 seconds
                        new KeyValue(wordProgress.progressProperty(), wordProgress.getProgress())
                ),
                new KeyFrame(
                        Duration.seconds(0.5), // Animate over 2 seconds
                        new KeyValue(wordProgress.progressProperty(), progress)
                )
        );

        timeline.play();
    }
}
