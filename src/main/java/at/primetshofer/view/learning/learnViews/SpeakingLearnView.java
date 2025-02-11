package at.primetshofer.view.learning.learnViews;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.Controller;
import at.primetshofer.model.STT;
import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.entities.SentenceWord;
import at.primetshofer.model.util.LangController;
import at.primetshofer.model.util.StringSimilarity;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpeakingLearnView extends LearnView {

    private static final String SPEAKING_PATH = "audio/system/recording.wav";
    private final Sentence sentence;
    private String speakingTranscription;
    private Label accuracyLabel;
    private Button recordButton;
    private int tries;
    private ChangeListener<Boolean> listener;
    private Map<String, SentenceWord> japaneseParts;
    private BorderPane bp;

    public SpeakingLearnView(LearnSessionManager learnSessionManager, Sentence sentence) {
        super(learnSessionManager, false);
        this.sentence = sentence;
        tries = 0;
        super.setDisableOverwrite(true);

        List<SentenceWord> sentenceWords = sentence.getSentenceWords();

        if (sentenceWords != null && !sentenceWords.isEmpty() && sentence.getJapanese() != null) {
            Map<String, SentenceWord> sentenceWordStrings = new LinkedHashMap<>();
            for (SentenceWord sentenceWord : sentenceWords) {
                if (sentenceWord.getWordJapanese() != null && !sentenceWord.getWordJapanese().isBlank()) {
                    sentenceWordStrings.put(sentenceWord.getWordJapanese(), sentenceWord);
                }
                if (sentenceWord.getWordEnglish() != null && !sentenceWord.getWordEnglish().isBlank()) {
                    sentenceWordStrings.put(sentenceWord.getWordEnglish(), sentenceWord);
                }
            }
            List<String> stringParts = ViewUtils.splitByDelimiters(sentence.getJapanese(), sentenceWordStrings.keySet());
            japaneseParts = new LinkedHashMap<>();

            for (String stringPart : stringParts) {
                japaneseParts.put(stringPart, sentenceWordStrings.getOrDefault(stringPart, null));
            }
        }
    }

    public static String extractText(String input) {
        // Parse the JSON input
        JSONObject jsonObject = new JSONObject(input);
        // Extract the value of the "text" key
        return jsonObject.getString("text");
    }

    @Override
    public Pane initView() {
        bp = new BorderPane();

        HBox textBox = new HBox();
        textBox.setSpacing(10);
        textBox.setAlignment(Pos.CENTER);

        if (sentence.getTtsPath() != null) {
            Image audioImage = new Image("audio.png");

            ImageView audioImageView = new ImageView(audioImage);
            audioImageView.setFitHeight(50);
            audioImageView.setFitWidth(50);

            Button audioButton = new Button();
            audioButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            audioButton.setGraphic(audioImageView);
            audioButton.setOnAction(e -> playSentenceTTS());
            textBox.getChildren().add(audioButton);
        }

        if (japaneseParts != null && !japaneseParts.isEmpty()) {
            HBox textLabels = new HBox();
            textLabels.setAlignment(Pos.CENTER);
            for (Map.Entry<String, SentenceWord> toTranslatePart : japaneseParts.entrySet()) {
                Label textLabel = new Label(toTranslatePart.getKey());
                textLabel.setStyle("-fx-font-size: 20pt");
                textLabels.getChildren().add(textLabel);

                if (toTranslatePart.getValue() != null) {
                    Tooltip tooltip = new Tooltip(LangController.getText("EnglishLabel") + " " + toTranslatePart.getValue().getWordEnglish() +
                            "\n" + LangController.getText("JapaneseLabel") + " " + toTranslatePart.getValue().getWordJapanese() +
                            "\n" + LangController.getText("KanaLabel") + " " + toTranslatePart.getValue().getWordKana());
                    tooltip.setStyle("-fx-font-size: 16pt");
                    textLabel.setUnderline(true);
                    Tooltip.install(textLabel, tooltip);
                }
            }
            textBox.getChildren().add(textLabels);
            textLabels.setSpacing(2);
        } else {
            Label textLabel = new Label(sentence.getJapanese());
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-font-size: 20pt");
            textBox.getChildren().add(textLabel);
        }

        recordButton = new Button(LangController.getText("StartRecording"));
        recordButton.getStyleClass().add("menuButton");
        recordButton.setUserData(false);
        recordButton.setOnAction(actionEvent -> {
            animateButtonClick(recordButton);
            boolean recording = (boolean) recordButton.getUserData();
            if (recording) {
                recordButton.setDisable(true);
                recordButton.setText(LangController.getText("AnalyseRecording"));
                AudioRecorder.stopRecording(SPEAKING_PATH);

                STT stt = STT.getStt();

                listener = (observableValue, oldValue, newValue) -> {
                    if (newValue) {
                        stt.sttCompletedProperty().removeListener(listener);
                        speakingTranscription = stt.getTranscript();
                        checkComplete();
                    }
                };

                stt.sttCompletedProperty().addListener(listener);

                new Thread(() -> {
                    stt.convertAudio(SPEAKING_PATH);
                }).start();
            } else {
                recordButton.setText(LangController.getText("StopRecording"));
                AudioRecorder.startRecording();
            }

            recordButton.setUserData(!recording);
        });

        accuracyLabel = new Label();
        accuracyLabel.setVisible(false);
        accuracyLabel.getStyleClass().add("sessionCompleteText");
        BorderPane.setAlignment(accuracyLabel, Pos.CENTER);

        VBox vb = new VBox(textBox, recordButton);
        vb.setAlignment(Pos.CENTER);
        vb.setSpacing(80);

        bp.setCenter(vb);
        bp.setBottom(accuracyLabel);

        return bp;
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

    @Override
    public void checkComplete() {
        tries++;
        speakingTranscription = extractText(speakingTranscription);

        double similarity = StringSimilarity.calculateSimilarity(speakingTranscription, sentence.getJapanese());

        Platform.runLater(() -> {
            accuracyLabel.setText(LangController.getText("AccuracyLabel") + " " + (int) similarity + "%");
            accuracyLabel.setVisible(true);

            if (similarity < 70) {
                if (tries >= 3) {
                    finished(false, sentence.getEnglish());
                } else {
                    recordButton.setText(LangController.getText("StartRecording"));
                    recordButton.setDisable(false);
                }
            } else {
                finished(true, sentence.getEnglish());
            }
        });
    }

    @Override
    public Pane resetView() {
        recordButton.setDisable(false);
        super.setDisableOverwrite(true);
        tries = 0;
        playSentenceTTS();
        return bp;
    }

    public void playSentenceTTS() {
        if (sentence.getTtsPath() == null) {
            return;
        }

        Controller.getInstance().playAudio(sentence.getTtsPath());
    }
}
