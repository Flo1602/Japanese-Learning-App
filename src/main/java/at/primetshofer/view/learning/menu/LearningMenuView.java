package at.primetshofer.view.learning.menu;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.util.DiscordActivityUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.services.LoadLearningDataService;
import at.primetshofer.view.MainMenuView;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.LoadingView;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnSessionManagers.QuestionSessionManager;
import at.primetshofer.view.learning.learnSessionManagers.SentenceSessionManager;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

public class LearningMenuView extends View {

    private static final Logger logger = Logger.getLogger(LearningMenuView.class);

    private KanjiMenuLearningView kanjiMenuLearningView;
    private WordMenuLearningView wordMenuLearningView;
    private final LoadLearningDataService loadLearningDataService;

    public LearningMenuView(Scene scene) {
        super(scene);

        loadLearningDataService = new LoadLearningDataService();

        loadLearningDataService.setOnFailed(event -> {
            logger.fatal("Error while loading learning data", event.getSource().getException());
            ViewUtils.showAlert(Alert.AlertType.ERROR,
                    LangController.getText("LearningDataLoadError"),
                    LangController.getText("FatalError"));
        });

        loadLearningDataService.setOnSucceeded(null);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("LearningHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);
        Button questionButton = new Button(LangController.getText("QuestionsButton"));
        questionButton.getStyleClass().add("smallMenuButton");
        questionButton.setOnAction(e -> {
            //QuestionSessionManager questionSessionManager = new QuestionSessionManager(scene);
            //questionSessionManager.initView();
            //questionSessionManager.display(this);
            logger.warn(QuestionSessionManager.class.getName() + " is not fully implemented yet");
            ViewUtils.showAlert(Alert.AlertType.WARNING,
                    LangController.getText("LearningModeNotImpl"),
                    LangController.getText("NotAvailableError"));
        });
        Button sentenceButton = new Button(LangController.getText("SentencesButton"));
        sentenceButton.getStyleClass().add("smallMenuButton");
        sentenceButton.setOnAction(e -> {
            //SentenceSessionManager sentenceSessionManager = new SentenceSessionManager(scene);
            //sentenceSessionManager.initView();
            //sentenceSessionManager.display(this);
            logger.warn(SentenceSessionManager.class.getName() + " is not fully implemented yet");
            ViewUtils.showAlert(Alert.AlertType.WARNING,
                    LangController.getText("LearningModeNotImpl"),
                    LangController.getText("NotAvailableError"));
        });

        Button vocabButton = new Button(LangController.getText("VocabButton"));
        vocabButton.getStyleClass().add("smallMenuButton");
        vocabButton.setOnAction(e -> {
            if (wordMenuLearningView == null) {
                wordMenuLearningView = new WordMenuLearningView(scene);
                wordMenuLearningView.initView();
            }

            showLearningSubMenu(wordMenuLearningView);
        });

        Button kanjiButton = new Button(LangController.getText("KanjiButton"));
        kanjiButton.getStyleClass().add("smallMenuButton");
        kanjiButton.setOnAction(e -> {
            if (kanjiMenuLearningView == null) {
                kanjiMenuLearningView = new KanjiMenuLearningView(scene);
                kanjiMenuLearningView.initView();
            }

            showLearningSubMenu(kanjiMenuLearningView);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(questionButton, sentenceButton, vocabButton, kanjiButton);
        BorderPane.setAlignment(vb, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        HBox bottom = new HBox();
        bottom.setSpacing(20);
        bottom.setAlignment(Pos.CENTER);

        Image mobileImage = new Image("tablet.png");

        ImageView mobileImageView = new ImageView(mobileImage);
        mobileImageView.setFitHeight(50);
        mobileImageView.setFitWidth(50);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(vb);
        bp.setBottom(bottom);

        Image settingsImage = new Image("stats.png");

        ImageView settingsImageView = new ImageView(settingsImage);
        settingsImageView.setFitHeight(50);
        settingsImageView.setFitWidth(50);

        Button settings = new Button();
        settings.getStyleClass().add("settingsButton");
        settings.setGraphic(settingsImageView);
        settings.setOnAction(e -> {
            StatsView statsView = new StatsView(scene);
            statsView.display(this);
        });

        HBox statsBox = new HBox(settings);
        statsBox.getStyleClass().add("container");
        statsBox.setAlignment(Pos.BOTTOM_CENTER);

        bp.setRight(statsBox);
    }

    private void showLearningSubMenu(View view) {
        if (view == null) {
            logger.error("Cannot show Null View!");
            throw new RuntimeException("Cannot show Null View!");
        }

        if (loadLearningDataService.getState() == Worker.State.SUCCEEDED) {
            view.display(this);
        } else {
            LoadingView loadingView = new LoadingView(scene);
            loadingView.setProgress(-1);
            loadingView.display(this);

            if (loadLearningDataService.getState() == Worker.State.CANCELLED || loadLearningDataService.getState() == Worker.State.FAILED) {
                loadLearningDataService.reset();
                loadLearningDataService.start();
            }

            if (loadLearningDataService.getState() == Worker.State.RUNNING) {
                loadLearningDataService.setOnSucceeded(event -> {
                    view.display(this);
                    loadLearningDataService.setOnSucceeded(null);
                });
            } else {
                view.display(this);
            }
        }
    }

    @Override
    public void display(View origin) {
        super.display(origin);

        AudioRecorder.stopRecordingThread();

        DiscordActivityUtil.setActivityDetails("Preparing to learn");

        if(origin instanceof MainMenuView){
            if (loadLearningDataService.getState() == Worker.State.RUNNING) {
                loadLearningDataService.cancel();
            }
            loadLearningDataService.reset();
            loadLearningDataService.start();
        }
    }
}
