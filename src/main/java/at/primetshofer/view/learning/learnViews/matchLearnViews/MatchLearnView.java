package at.primetshofer.view.learning.learnViews.matchLearnViews;

import at.primetshofer.model.Controller;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import at.primetshofer.view.learning.learnViews.LearnView;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class MatchLearnView extends LearnView {

    private static final int MAX_ERRORS = 5;

    private Map<String, String> matchPairBackup;
    private Map<String, String> matchPairs;
    private Map<String, String> ttsPaths;
    private boolean ttsOnly;

    private ToggleButton leftSelection = null;
    private ToggleButton rightSelection = null;
    private BooleanProperty disableButton;

    private Controller controller;
    private int errors = 0;

    public MatchLearnView(LearnSessionManager learnSessionManager) {
        super(learnSessionManager, false);
        this.controller = Controller.getInstance();
        disableButton = new SimpleBooleanProperty(false);
    }

    @Override
    public Pane initView() {
        ArrayList<String> keyList = new ArrayList<>(matchPairs.keySet());
        ArrayList<String> valueList = new ArrayList<>(matchPairs.values());
        Collections.shuffle(keyList);
        Collections.shuffle(valueList);

        VBox vbox = new VBox();

        Image audioImage = new Image("audio.png");

        for (int i = 0; i < keyList.size(); i++) {
            ToggleButton leftButton = new ToggleButton(keyList.get(i));
            leftButton.disableProperty().bind(disableButton);
            if(ttsOnly){
                leftButton.setText("");
                ImageView audioImageView = new ImageView(audioImage);
                audioImageView.setFitHeight(35);
                audioImageView.setFitWidth(35);
                leftButton.setGraphic(audioImageView);
            }
            leftButton.setUserData(keyList.get(i));
            leftButton.getStyleClass().add("matchButton");
            int finalI = i;
            leftButton.setOnAction(event -> {
                if(ttsPaths != null && ttsPaths.containsKey(keyList.get(finalI))){
                    controller.playAudio(ttsPaths.get(keyList.get(finalI)));
                }
                if (leftButton.isSelected()) {
                    animateButtonClick(leftButton);
                    if (leftSelection != null) {
                        leftSelection.setSelected(false);
                    }
                    leftSelection = leftButton;
                    checkSelection();
                } else {
                    leftSelection = null;
                }
            });

            ToggleButton rightButton = new ToggleButton(valueList.get(i));
            rightButton.disableProperty().bind(disableButton);
            rightButton.setUserData(valueList.get(i));
            rightButton.getStyleClass().add("matchButton");
            rightButton.setOnAction(event -> {
                if(ttsPaths != null && ttsPaths.containsKey(valueList.get(finalI))){
                    controller.playAudio(ttsPaths.get(valueList.get(finalI)));
                }
                if (rightButton.isSelected()) {
                    animateButtonClick(rightButton);
                    if (rightSelection != null) {
                        rightSelection.setSelected(false);
                    }
                    rightSelection = rightButton;
                    checkSelection();
                } else {
                    rightSelection = null;
                }
            });

            BorderPane match = new BorderPane();
            match.setLeft(leftButton);
            match.setRight(rightButton);

            vbox.getChildren().add(match);
        }

        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(30);
        vbox.setPadding(new Insets(0, 100, 0, 100));

        return vbox;
    }

    private void animateButtonClick(ToggleButton btn) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), btn);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.1);
        scaleTransition.setToY(1.1);
        scaleTransition.setCycleCount(2);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
    }

    private void applyCorrectAnimation(ToggleButton btn) {
        btn.getStyleClass().add("correctAnswerButton");
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(250), btn);
        fadeTransition.setFromValue(0.5);
        fadeTransition.setToValue(1.0);
        fadeTransition.setCycleCount(2);
        fadeTransition.setAutoReverse(true);
        fadeTransition.setOnFinished(event -> {
            btn.disableProperty().unbind();
            btn.setDisable(true);
            btn.getStyleClass().remove("correctAnswerButton");
        });
        fadeTransition.play();
    }

    private void applyIncorrectAnimation(ToggleButton btn) {
        btn.getStyleClass().add("wrongAnswerButton");
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(50), btn);
        translateTransition.setFromX(0);
        translateTransition.setByX(10);
        translateTransition.setCycleCount(6);
        translateTransition.setAutoReverse(true);
        translateTransition.setOnFinished(event -> btn.getStyleClass().remove("wrongAnswerButton"));
        translateTransition.play();
    }

    private void checkSelection() {
        if (leftSelection != null && rightSelection != null) {
            if (matchPairs.get((String) leftSelection.getUserData()).equals(rightSelection.getUserData())) {
                applyCorrectAnimation(leftSelection);
                applyCorrectAnimation(rightSelection);

                matchPairs.remove((String) leftSelection.getUserData());
            } else {
                errors++;
                applyIncorrectAnimation(leftSelection);
                applyIncorrectAnimation(rightSelection);
            }
            unselectButtons();
            checkComplete();
        }
    }

    private void unselectButtons() {
        leftSelection.setSelected(false);
        rightSelection.setSelected(false);
        rightSelection = null;
        leftSelection = null;
    }

    @Override
    public void checkComplete() {
        if (matchPairs.isEmpty()){
            super.finished(true);
        }
        if(errors >= 5){
            disableButton.set(true);
            super.finished(false);
        }
    }

    public void setMatchPairs(Map<String, String> matchPairs) {
        this.matchPairs = matchPairs;
        this.matchPairBackup = new HashMap<>(matchPairs);
    }

    public void setTtsPaths(Map<String, String> ttsPaths, boolean ttsOnly) {
        this.ttsPaths = ttsPaths;
        this.ttsOnly = ttsOnly;
    }

    @Override
    public Pane resetView() {
        this.matchPairs = new HashMap<>(matchPairBackup);
        disableButton.set(false);
        errors = 0;
        return initView();
    }
}
