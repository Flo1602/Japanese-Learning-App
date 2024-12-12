package at.primetshofer.view.learning.learnViews;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Question;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.animation.ScaleTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class QuestionLearnView extends LearnView {

    private Question question;
    private boolean ttsEnabled;
    private boolean textEnabled;
    private BooleanProperty disableButton;
    private VBox vbox;

    private ToggleButton selectedButton;

    public QuestionLearnView(LearnSessionManager learnSessionManager, Question question, boolean ttsEnabled, boolean textEnabled) {
        super(learnSessionManager, true);

        if(!ttsEnabled && !textEnabled) {
            throw new IllegalArgumentException("ttsEnabled and textEnabled cannot be both false");
        }

        this.question = question;
        this.ttsEnabled = ttsEnabled;
        this.textEnabled = textEnabled;
        disableButton = new SimpleBooleanProperty(false);
    }

    @Override
    public Pane initView() {
        vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(20);

        HBox textBox = new HBox();
        textBox.setSpacing(10);
        textBox.setAlignment(Pos.CENTER);

        if(ttsEnabled) {
            Image audioImage = new Image("audio.png");

            ImageView audioImageView = new ImageView(audioImage);
            audioImageView.setFitHeight(50);
            audioImageView.setFitWidth(50);

            Button audioButton = new Button();
            audioButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            audioButton.setGraphic(audioImageView);
            audioButton.setOnAction(e -> playQuestionTTS());
            textBox.getChildren().add(audioButton);
        }

        if(textEnabled) {
            Label textLabel = new Label(question.getJapanese());
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-font-size: 20pt");
            textBox.getChildren().add(textLabel);
        }

        vbox.getChildren().add(textBox);

        Label questionLabel = new Label(question.getQuestion());
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-font-size: 16pt");
        vbox.getChildren().add(questionLabel);

        for (String answer : question.getAnswers()) {
            ToggleButton answerButton = new ToggleButton(answer);
            answerButton.getStyleClass().add("answerButton");
            answerButton.disableProperty().bind(disableButton);
            answerButton.setOnAction(event -> {
                if (answerButton.isSelected()) {
                    animateButtonClick(answerButton);
                    if (selectedButton != null) {
                        selectedButton.setSelected(false);
                    } else {
                        super.activateCheckButton(true);
                    }
                    selectedButton = answerButton;
                } else {
                    super.activateCheckButton(false);
                    selectedButton = null;
                }
            });

            vbox.getChildren().add(answerButton);
        }

        return vbox;
    }

    @Override
    public void checkComplete() {
        Controller.getInstance().stopAudio();
        disableButton.set(true);

        if(selectedButton.getText().equals(question.getCorrectAnswer())){
            super.finished(true);
        } else {
            super.finished(false);
        }
    }

    @Override
    public Pane resetView() {
        disableButton.set(false);
        selectedButton.setSelected(false);
        selectedButton = null;
        super.setCheckButtonVisible(true);
        playQuestionTTS();
        return vbox;
    }

    public void playQuestionTTS(){
        if(!ttsEnabled){
            return;
        }
        Controller.getInstance().playAudio(question.getTtsPath());
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
}
