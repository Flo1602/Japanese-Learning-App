package at.primetshofer.view.learning.menu;

import at.primetshofer.model.Controller;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.CreateEditWordWindow;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnSessionManagers.KanjiSessionManager;
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
import org.apache.log4j.Logger;

public class KanjiMenuLearningView extends View {

    private static final Logger logger = Logger.getLogger(KanjiMenuLearningView.class);

    private final Controller controller;
    private ProgressBar kanjiProgress;
    private Label kanjiProgressLabel;
    private Button addKanjiToDue;
    private NetworkLearningView networkLearningView;
    private SelectKanjiLearningView selectKanjiLearningView;
    private KanjiSessionManager kanjiSessionManager;

    public KanjiMenuLearningView(Scene scene) {
        super(scene);
        controller = Controller.getInstance();
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("KanjiButton"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        Button dailyKanjiButton = new Button(LangController.getText("DailyKanjiButton"));
        dailyKanjiButton.getStyleClass().add("menuButton");
        dailyKanjiButton.setOnAction(e -> {
            if (kanjiSessionManager == null) {
                kanjiSessionManager = new KanjiSessionManager(scene);
            }

            kanjiSessionManager.initSessionManager();
            kanjiSessionManager.initView();
            kanjiSessionManager.display(this);
        });

        Button selectKanjiButton = new Button(LangController.getText("SelectKanji"));
        selectKanjiButton.getStyleClass().add("menuButton");
        selectKanjiButton.setOnAction(e -> {
            if (kanjiSessionManager == null) {
                kanjiSessionManager = new KanjiSessionManager(scene);
            }
            if (selectKanjiLearningView == null) {
                selectKanjiLearningView = new SelectKanjiLearningView(scene, kanjiSessionManager);
            }

            selectKanjiLearningView.display(this);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(dailyKanjiButton, selectKanjiButton);
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
            if (event.getButton() == MouseButton.MIDDLE) {
                KanjiCheatView kanjiCheatView = new KanjiCheatView(scene);
                kanjiCheatView.display(this);
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
            if (networkLearningView == null) {
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
        kanjiProgressLabel.setText(controller.getDueKanjiCount() + " " + LangController.getText("KanjiProgressLabel") + " (" + controller.getDueTotalKanjiCount() + " " + LangController.getText("TotalLabel") + "):");

        setProgress(controller.getKanjiProgress());

        addKanjiToDue.setVisible(controller.getDueKanjiCount() != controller.getDueTotalKanjiCount());
    }

    public void setProgress(double progress) {
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
