package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.Controller;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnViews.KanjiTracerLearnView;
import at.primetshofer.view.learning.learnViews.LearnView;
import at.primetshofer.view.learning.learnViews.WordDefense;
import at.primetshofer.view.learning.menu.SessionCompletedView;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class LearnSessionManager {

    private final static String SUCCESS_AUDIO = "audio\\system\\success.mp3";
    private final static String FAIL_AUDIO = "audio\\system\\fail.mp3";
    private final static String FINISHED_AUDIO = "audio\\system\\finished.mp3";

    protected Scene scene;
    protected BorderPane bp;
    protected ObjectProperty<View> origin;
    protected LearnView currentLearnView;

    private Button continueButton;
    private Button overwriteCorrectness;
    private boolean checkButton;
    private boolean disableOverwrite;
    private Label infoLabel;
    private Label messageLabel;
    private VBox infoBox;
    private BorderPane bottomArea;
    private ProgressBar progressBar;
    private final Controller controller;

    private LocalTime startTIme;
    private List<Boolean> successList;
    private Queue<LearnView> wrongList;
    private int maxViews;
    private int correctCounter;
    private boolean correctMistakesMode;

    public LearnSessionManager(Scene scene) {
        this.scene = scene;
        this.controller = Controller.getInstance();
    }

    public void initSessionManager() {
        origin = new SimpleObjectProperty<>();
        successList = new ArrayList<>();
        checkButton = false;
        disableOverwrite = false;
        correctMistakesMode = false;
        wrongList = new ArrayDeque<>();
        correctCounter = 0;
    }

    protected abstract void startLearning();

    protected abstract void nextLearningView();

    public void initView() {
        bp = new BorderPane();
        HBox backButtonBox = ViewUtils.getBackButtonBox(origin);
        backButtonBox.getStyleClass().clear();
        backButtonBox.getStyleClass().add("container3");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        progressBar.setPrefHeight(60);
        progressBar.setPadding(new Insets(20, 0, 0, 0));
        BorderPane.setAlignment(progressBar, Pos.CENTER);

        bp.setLeft(backButtonBox);

        Region spacer = new Region();
        backButtonBox.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bottomArea = getBottomArea();

        bp.setRight(spacer);
        bp.setTop(progressBar);
        bp.setBottom(bottomArea);
    }

    private BorderPane getBottomArea() {
        BorderPane bottomArea = new BorderPane();
        bottomArea.setPrefHeight(110);

        Rectangle line = new Rectangle(0, 1, Color.GRAY);
        line.widthProperty().bind(scene.widthProperty());

        continueButton = new Button(LangController.getText("ContinueButton"));
        continueButton.setDisable(true);
        continueButton.getStyleClass().add("normalButton");
        continueButton.setOnAction(event -> {
            if (checkButton) {
                currentLearnView.checkComplete();
                checkButton = false;
                continueButton.setText(LangController.getText("ContinueButton"));
                return;
            }
            continueButton.setDisable(true);
            overwriteCorrectness.setVisible(false);
            infoLabel.setVisible(false);
            messageLabel.setVisible(false);
            bottomArea.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
            infoBox.getChildren().remove(messageLabel);
            disableOverwrite = false;
            nextLearningView();
        });
        HBox nextLearnBox = new HBox(continueButton);
        nextLearnBox.setAlignment(Pos.CENTER_RIGHT);
        nextLearnBox.setPadding(new Insets(0, 50, 0, 0));

        infoLabel = new Label();
        infoLabel.setVisible(false);
        messageLabel = new Label();
        messageLabel.setVisible(false);
        messageLabel.getStyleClass().add("smallText");
        infoBox = new VBox(infoLabel);
        infoBox.setAlignment(Pos.CENTER);

        bottomArea.setTop(line);
        bottomArea.setRight(nextLearnBox);
        bottomArea.setCenter(infoBox);

        overwriteCorrectness = new Button(LangController.getText("AddSynonymButton"));
        overwriteCorrectness.setVisible(false);
        overwriteCorrectness.getStyleClass().add("smallButton");
        overwriteCorrectness.setOnAction(event -> {
            overwriteCorrectness.setDisable(true);
            if (!correctMistakesMode) {
                successList.remove(successList.getLast());
            }
            learnViewFinished(true);
            currentLearnView.correctnessOverwritten();
        });
        HBox overwriteBox = new HBox(overwriteCorrectness);
        overwriteBox.setAlignment(Pos.CENTER_RIGHT);

        nextLearnBox.widthProperty().addListener((observableValue, oldValue, newValue) -> overwriteBox.setPrefWidth(newValue.doubleValue()));

        bottomArea.setLeft(overwriteBox);

        return bottomArea;
    }

    public void learnViewFinished(boolean success) {
        learnViewFinished(success, null);
    }

    public void learnViewFinished(boolean success, String message) {
        if (!correctMistakesMode) {
            successList.add(success);
        }
        continueButton.setDisable(false);
        infoLabel.setVisible(true);
        infoLabel.getStyleClass().clear();
        if (message != null) {
            messageLabel.setText(message);
            messageLabel.setVisible(true);
            infoBox.getChildren().add(messageLabel);

            if (!disableOverwrite) {
                overwriteCorrectness.setVisible(true);
                overwriteCorrectness.setDisable(false);
            }
        }

        AtomicReference<Double> opacity = new AtomicReference<>(0.0);
        int cycles = 40;
        double increase = 1.0 / cycles;

        Timeline backgroundColorAnimation = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(5), event -> {
            double op = opacity.get();
            bottomArea.setBackground(new Background(new BackgroundFill(Color.color(0.141, 0.184, 0.196, op), null, null)));
            opacity.set(op + increase);
        });
        backgroundColorAnimation.getKeyFrames().add(keyFrame);
        backgroundColorAnimation.setCycleCount(cycles);

        if (success) {
            correctCounter++;
            setProgress((double) correctCounter / maxViews);
            controller.playAudio(SUCCESS_AUDIO);
            infoLabel.getStyleClass().add("correctText");
            infoLabel.setText(LangController.getText("CorrectText"));
        } else {
            if (currentLearnView instanceof KanjiTracerLearnView || currentLearnView instanceof WordDefense) {
                correctCounter++;
                setProgress((double) correctCounter / maxViews);
            } else {
                wrongList.add(currentLearnView);
            }
            controller.playAudio(FAIL_AUDIO);
            infoLabel.getStyleClass().add("wrongText");
            infoLabel.setText(LangController.getText("WrongText"));
        }

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), infoLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        backgroundColorAnimation.play();
        fadeIn.play();

        if (message != null) {
            FadeTransition fadeIn2 = new FadeTransition(Duration.seconds(0.2), messageLabel);
            fadeIn2.setFromValue(0.0);
            fadeIn2.setToValue(1.0);
            fadeIn2.play();

            if (!disableOverwrite) {
                FadeTransition fadeIn3 = new FadeTransition(Duration.seconds(0.2), overwriteCorrectness);
                fadeIn3.setFromValue(0.0);
                fadeIn3.setToValue(1.0);
                fadeIn3.play();
            }
        }
    }

    protected void learnSessionFinished() {
        if (!wrongList.isEmpty()) {
            correctMistakesMode = true;
            currentLearnView = wrongList.poll();
            bp.setCenter(currentLearnView.resetView());
            return;
        }

        java.time.Duration duration = java.time.Duration.between(startTIme, LocalTime.now());

        double percent = 0;
        double percentValue = 100.0 / successList.size();

        for (Boolean b : successList) {
            if (b) {
                percent += percentValue;
            }
        }

        if (percent == 0) {
            percent = 1;
        }

        updateProgresses((int) percent);

        SessionCompletedView sessionCompletedView = new SessionCompletedView(scene, duration, (int) percent);
        sessionCompletedView.display(origin.get());
        controller.playAudio(FINISHED_AUDIO);
    }

    protected abstract void updateProgresses(int percent);

    public void display(View origin) {
        if (origin != null) {
            this.origin.set(origin);
        }
        scene.setRoot(bp);
        startTIme = LocalTime.now();
        startLearning();
    }

    private void setProgress(double progress) {
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.ZERO, // Start at 0 seconds
                        new KeyValue(progressBar.progressProperty(), progressBar.getProgress())
                ),
                new KeyFrame(
                        Duration.seconds(0.25), // Animate over 2 seconds
                        new KeyValue(progressBar.progressProperty(), progress)
                )
        );

        timeline.play();
    }

    public void changeContinueToCheck() {
        checkButton = true;
        continueButton.setText(LangController.getText("CheckButton"));
    }

    public void activateCheckButton(boolean activate) {
        continueButton.setDisable(!activate);
    }

    public void setDisableOverwrite(boolean disableOverwrite) {
        this.disableOverwrite = disableOverwrite;
    }

    public void setMaxViews(int maxViews) {
        this.maxViews = maxViews;
    }
}
