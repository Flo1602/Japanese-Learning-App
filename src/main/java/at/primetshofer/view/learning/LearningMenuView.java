package at.primetshofer.view.learning;

import at.primetshofer.model.Controller;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnSessionManagers.*;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LearningMenuView extends View {

    private ProgressBar kanjiProgress;
    private Label kanjiProgressLabel;
    private Controller controller;
    private Button addKanjiToDue;
    private NetworkLearningView networkLearningView;
    private SelectKanjiLearningView selectKanjiLearningView;
    private KanjiSessionManager kanjiSessionManager;

    public LearningMenuView(Scene scene) {
        super(scene);
        controller = Controller.getInstance();
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("LearningHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        Button wordsButton = new Button(LangController.getText("WordsButton"));
        wordsButton.getStyleClass().add("smallMenuButton");
        wordsButton.setOnAction(e -> {
            WordSessionManager wordSessionManager = new WordSessionManager(scene);
            wordSessionManager.initView();
            wordSessionManager.display(this);
        });

        Button questionButton = new Button("Questions");
        questionButton.getStyleClass().add("smallMenuButton");
        questionButton.setOnAction(e -> {
            QuestionSessionManager questionSessionManager = new QuestionSessionManager(scene);
            questionSessionManager.initView();
            questionSessionManager.display(this);
        });

        Button sentenceButton = new Button("Sentences");
        sentenceButton.getStyleClass().add("smallMenuButton");
        sentenceButton.setOnAction(e -> {
            SentenceSessionManager sentenceSessionManager = new SentenceSessionManager(scene);
            sentenceSessionManager.initView();
            sentenceSessionManager.display(this);
        });

        Button speakingButton = new Button("Speaking");
        speakingButton.getStyleClass().add("smallMenuButton");
        speakingButton.setOnAction(e -> {
            SpeakingSessionManager speakingSessionManager = new SpeakingSessionManager(scene);
            speakingSessionManager.initView();
            speakingSessionManager.display(this);
        });

        Button dailyKanjiButton = new Button(LangController.getText("DailyKanjiButton"));
        dailyKanjiButton.getStyleClass().add("smallMenuButton");
        dailyKanjiButton.setOnAction(e -> {
            if(kanjiSessionManager == null) {
                kanjiSessionManager = new KanjiSessionManager(scene);
            }

            kanjiSessionManager.initSessionManager();
            kanjiSessionManager.initView();
            kanjiSessionManager.display(this);
        });

        Button selectKanjiButton = new Button(LangController.getText("SelectKanji"));
        selectKanjiButton.getStyleClass().add("smallMenuButton");
        selectKanjiButton.setOnAction(e -> {
            if(kanjiSessionManager == null) {
                kanjiSessionManager = new KanjiSessionManager(scene);
            }
            if(selectKanjiLearningView == null) {
                selectKanjiLearningView = new SelectKanjiLearningView(scene, kanjiSessionManager);
            }

            selectKanjiLearningView.display(this);
        });

        HBox kanjiLearningButtons = new HBox(dailyKanjiButton, selectKanjiButton);
        kanjiLearningButtons.setSpacing(25);
        kanjiLearningButtons.setAlignment(Pos.CENTER);

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(wordsButton, questionButton, sentenceButton, speakingButton, kanjiLearningButtons);
        BorderPane.setAlignment(vb, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        HBox bottom = new HBox();
        bottom.setSpacing(20);
        bottom.setAlignment(Pos.CENTER);

        addKanjiToDue = new Button("+");
        addKanjiToDue.getStyleClass().add("plusButton");

        addKanjiToDue.setOnAction(e -> addKanjiToDueAction());

        kanjiProgressLabel = new Label(LangController.getText("KanjiProgressLabel"));
        kanjiProgressLabel.getStyleClass().add("smallText");

        kanjiProgress = new ProgressBar();
        kanjiProgress.setProgress(0);
        kanjiProgress.setPrefSize(200, 25);
        kanjiProgress.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.MIDDLE){
                CheatView cheatView = new CheatView(scene);
                cheatView.display(this);
            }
        });

        bottom.getChildren().addAll(addKanjiToDue, kanjiProgressLabel, kanjiProgress);

        Image mobileImage = new Image("tablet.png");

        ImageView mobileImageView = new ImageView(mobileImage);
        mobileImageView.setFitHeight(50);
        mobileImageView.setFitWidth(50);

        Button mobileLink = new Button();
        mobileLink.getStyleClass().add("settingsButton");
        mobileLink.setGraphic(mobileImageView);
        mobileLink.setOnAction(e -> {
            if(networkLearningView == null) {
                networkLearningView = new NetworkLearningView(scene);
            }
            networkLearningView.display(this);
        });

        HBox mobileLinkBox = new HBox(mobileLink);
        mobileLinkBox.getStyleClass().add("container2");
        mobileLinkBox.setAlignment(Pos.TOP_LEFT);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setRight(mobileLinkBox);
        bp.setCenter(vb);
        bp.setBottom(bottom);

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> mobileLinkBox.setPrefWidth(newValue.doubleValue()));
    }

    private void addKanjiToDueAction() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                controller.increaseDueKanjiTmp(1);
                controller.updateKanjiList();
                return null;
            }
        };

        task.setOnSucceeded(event -> updateProgress());
        task.setOnFailed(event -> {
           event.getSource().getException().printStackTrace();
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
        kanjiProgressLabel.setText(controller.getDueKanjiCount() + " " + LangController.getText("KanjiProgressLabel") + " (" + controller.getDueTotalKanjiCount() + " " + LangController.getText("TotalLabel") + "):");

        setProgress(controller.getKanjiProgress());

        addKanjiToDue.setVisible(controller.getDueKanjiCount() != controller.getDueTotalKanjiCount());
    }

    public void setProgress(double progress){
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.ZERO, // Start at 0 seconds
                        new KeyValue(kanjiProgress.progressProperty(), kanjiProgress.getProgress())
                ),
                new KeyFrame(
                        Duration.seconds(0.5), // Animate over 2 seconds
                        new KeyValue(kanjiProgress.progressProperty(), progress)
                )
        );

        timeline.play();
    }
}
