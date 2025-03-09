package at.primetshofer.view.learning.learnViews;

import at.primetshofer.logic.tracing.verification.VerificationLogic;
import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.Controller;
import at.primetshofer.model.KanaToRomajiConverter;
import at.primetshofer.model.STT;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.LangController;
import at.primetshofer.model.util.StringSimilarity;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WordDefense extends LearnView {

    private static final Logger logger = Logger.getLogger(WordDefense.class);
    private static final String SPEAKING_PATH = "audio/system/recording.wav";
    private ChangeListener<Boolean> listener;
    private final List<Word> words;
    private final List<Word> allWords;
    private Button recordButton;
    private final List<Label> attackers;
    private final IntegerProperty lives = new SimpleIntegerProperty(3);
    private boolean allSpawned = false;
    private final BooleanProperty disableButton;

    public WordDefense(LearnSessionManager learnSessionManager, List<Word> words) {
        super(learnSessionManager, false);
        this.words = words;
        this.attackers = new ArrayList<>();
        this.allWords = new ArrayList<>(words);
        this.disableButton = new SimpleBooleanProperty(false);

        Collections.shuffle(words);
    }

    private static void animateDespawn(Label label) {
        // Create a fade-out transition
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Create a scale transition for a shrinking effect
        ScaleTransition scaleDown = new ScaleTransition(Duration.seconds(1), label);
        scaleDown.setFromX(1.0);
        scaleDown.setFromY(1.0);
        scaleDown.setToX(0.2);
        scaleDown.setToY(0.2);

        // Create a rotation transition for an added effect
        RotateTransition rotate = new RotateTransition(Duration.seconds(1), label);
        rotate.setByAngle(360);

        // Combine animations into a sequential transition
        ParallelTransition despawnAnimation = new ParallelTransition(fadeOut, scaleDown, rotate);

        // Remove the label from its parent after the animation finishes
        despawnAnimation.setOnFinished(event -> {
            if (label.getParent() != null) {
                ((Pane) label.getParent()).getChildren().remove(label);
            }
        });

        // Start the animation
        despawnAnimation.play();
    }

    private static void animateSuccessDespawn(Label label) {
        // Create a scale-up transition for an "explosion" effect
        ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.5), label);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(2.0);
        scaleUp.setToY(2.0);

        // Create a fade-out transition after the scale-up
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Combine animations into a sequential transition
        ParallelTransition successDespawnAnimation = new ParallelTransition(fadeOut, scaleUp);

        // Remove the label from its parent after the animation finishes
        successDespawnAnimation.setOnFinished(event -> {
            if (label.getParent() != null) {
                ((Pane) label.getParent()).getChildren().remove(label);
            }
        });

        // Start the animation
        successDespawnAnimation.play();
    }

    private static void animateSpawn(Label label) {
        // Start with the label scaled down and transparent
        label.setScaleX(0.0);
        label.setScaleY(0.0);
        label.setOpacity(0.0);

        // Create a scale-up transition
        ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.5), label);
        scaleUp.setFromX(0.0);
        scaleUp.setFromY(0.0);
        scaleUp.setToX(1.2);
        scaleUp.setToY(1.2);

        // Create a subtle bounce-back for the "pop" effect
        ScaleTransition scaleBounceBack = new ScaleTransition(Duration.seconds(0.2), label);
        scaleBounceBack.setFromX(1.2);
        scaleBounceBack.setFromY(1.2);
        scaleBounceBack.setToX(1.0);
        scaleBounceBack.setToY(1.0);

        // Combine the scale-up and bounce-back
        SequentialTransition scaleAnimation = new SequentialTransition(scaleUp, scaleBounceBack);

        // Create a fade-in transition
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Combine scale and fade animations
        ParallelTransition spawnAnimation = new ParallelTransition(scaleAnimation, fadeIn);

        // Start the animation
        spawnAnimation.play();
    }

    @Override
    public Pane initView() {
        BorderPane bp = new BorderPane();

        Label livesLabel = new Label(LangController.getText("Lives") + " " + lives.getValue());
        livesLabel.getStyleClass().add("normalText");
        BorderPane.setAlignment(livesLabel, Pos.CENTER);

        lives.addListener((observableValue, oldValue, newValue) -> {
            if (lives.get() >= 0) {
                livesLabel.setText(LangController.getText("Lives") + " " + lives.getValue());
            } else {
                livesLabel.setText(LangController.getText("Lives") + " ☠");
            }
        });

        bp.setTop(livesLabel);

        recordButton = new Button(LangController.getText("StartLoading"));
        recordButton.disableProperty().bind(disableButton);
        recordButton.setUserData(false);
        recordButton.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-pref-width: 150");
        recordButton.setOnAction(actionEvent -> {
            animateButtonClick(recordButton);
            boolean recording = (boolean) recordButton.getUserData();
            if (recording) {
                disableButton.set(true);
                recordButton.setText(LangController.getText("PrepareFire"));
                AudioRecorder.stopRecording(SPEAKING_PATH);

                STT stt = STT.getStt();

                listener = (observableValue, oldValue, newValue) -> {
                    if (newValue) {
                        stt.sttCompletedProperty().removeListener(listener);
                        String shot = SpeakingLearnView.extractText(stt.getTranscript()).replaceAll(" ", "").replace("これは", "").replace("です", "");
                        String romaji = KanaToRomajiConverter.katakanaToRomaji(ViewUtils.convertKanjiToKatakana(shot));
                        if (romaji.isEmpty() || romaji.equals("*")) {
                            romaji = KanaToRomajiConverter.katakanaToRomaji(shot);
                        }

                        for (Label attacker : attackers) {
                            String attackerKana = "";

                            for (Word word : allWords) {
                                if (word.getJapanese().equals(attacker.getText())) {
                                    attackerKana = word.getKana();
                                }
                            }

                            String attackerRomaji = KanaToRomajiConverter.hiraganaToRomaji(attackerKana);

                            logger.info("Romaji: " + romaji);

                            double similarity = StringSimilarity.calculateSimilarity(shot, attacker.getText());
                            double similarity2 = StringSimilarity.calculateSimilarity(romaji, attackerRomaji);
                            double similarity3 = StringSimilarity.calculateSimilarity(shot, attackerKana);

                            logger.info("Similarity 1: " + similarity + ", Similarity 2: " + similarity2 + ", Similarity 3:" + similarity3);

                            if (similarity >= 50 || similarity2 >= 50 || similarity3 >= 50) {
                                if (destroyAttacker(attacker)) {
                                    return;
                                }

                                break;
                            }
                        }

                        Platform.runLater(() -> {
                            recordButton.setText(LangController.getText("StartLoading"));
                            disableButton.set(false);
                        });
                    }
                };

                stt.sttCompletedProperty().addListener(listener);

                new Thread(() -> {
                    stt.convertAudio(SPEAKING_PATH);
                }).start();
            } else {
                recordButton.setText(LangController.getText("StopLoading"));
                AudioRecorder.startRecording();
            }

            recordButton.setUserData(!recording);
        });

        HBox right = new HBox(recordButton);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, -100, 0, 100));

        bp.setCenter(getBattlefield());
        bp.setRight(right);

        return bp;
    }

    private boolean destroyAttacker(Label attacker) {
        ((TranslateTransition) attacker.getUserData()).stop();
        Platform.runLater(() -> animateSuccessDespawn(attacker));
        attackers.remove(attacker);

        if (attackers.isEmpty() && allSpawned) {
            disableButton.set(true);
            Platform.runLater(() -> finished(true));

            return true;
        }

        new Thread(() -> {
            for (Word word : allWords) {
                if (word.getJapanese().equals(attacker.getText())) {
                    Controller.getInstance().playAudio(word.getTtsPath());
                }
            }
        }).start();

        return false;
    }

    @Override
    public void checkComplete() {
        throw new IllegalStateException("checkComplete should never be called");
    }

    @Override
    public Pane resetView() {
        throw new UnsupportedOperationException("ResetView should not be called on WordDefense!");
    }

    private Pane getBattlefield() {
        Pane battlefield = new Pane();
        battlefield.setPrefHeight(500);

        Line line = new Line();
        line.setStroke(Color.RED);
        line.startXProperty().bind(battlefield.widthProperty());
        line.setStartY(0);
        line.endXProperty().bind(battlefield.widthProperty());
        line.endYProperty().bind(battlefield.heightProperty());

        battlefield.getChildren().add(line);

        Timeline spawner = new Timeline();
        spawner.setCycleCount(words.size());
        spawner.getKeyFrames().add(new KeyFrame(Duration.millis(10000), e -> {
            Word nextWord = words.getFirst();
            words.remove(nextWord);
            Label word = new Label(nextWord.getJapanese());
            word.getStyleClass().add("normalText");
            word.setLayoutX(0);
            word.setLayoutY(new Random().nextDouble(battlefield.getHeight() - 50));

            word.setOnMouseClicked(event -> destroyAttacker(word));

            battlefield.getChildren().add(word);
            attackers.add(word);

            animateSpawn(word);

            TranslateTransition transition = new TranslateTransition(Duration.millis(25000), word);
            transition.setToX(battlefield.getWidth());
            transition.setOnFinished(event -> {
                attackers.remove(word);
                if (lives.get() == 0) {
                    disableButton.set(true);
                    finished(false);
                    spawner.stop();
                    animateDespawn(word);
                }
                if (word.isVisible()) {
                    lives.set(lives.get() - 1);
                    animateDespawn(word);
                }

                if (attackers.isEmpty() && allSpawned) {
                    disableButton.set(true);
                    Platform.runLater(() -> finished(true));
                }
            });

            word.setUserData(transition);

            transition.play();
        }));

        spawner.setOnFinished(event -> allSpawned = true);

        spawner.play();

        return battlefield;
    }

    private void animateButtonClick(Button btn) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), btn);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.1);
        scaleTransition.setToY(1.1);
        scaleTransition.setCycleCount(2);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
    }
}
