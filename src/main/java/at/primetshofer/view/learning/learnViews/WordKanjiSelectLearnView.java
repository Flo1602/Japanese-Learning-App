package at.primetshofer.view.learning.learnViews;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import jakarta.persistence.EntityManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordKanjiSelectLearnView extends LearnView{

    private Word word;
    private BooleanProperty disableButton;
    private List<Kanji> correctKanjis;
    private List<Kanji> wrongKanjis;
    private int correctButtonNumber;

    private ToggleButton selectedButton;

    public WordKanjiSelectLearnView(LearnSessionManager learnSessionManager, Word word) {
        super(learnSessionManager, true);
        this.word = word;
        disableButton = new SimpleBooleanProperty(false);
        correctKanjis = new ArrayList<>(word.getKanjis());

        correctButtonNumber = new Random().nextInt(4);

        getWrongKanjis();

        super.setDisableOverwrite(true);
    }

    @Override
    public Pane initView() {
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(50);

        HBox textBox = new HBox();
        textBox.setSpacing(10);
        textBox.setAlignment(Pos.CENTER);

        Image audioImage = new Image("audio.png");

        ImageView audioImageView = new ImageView(audioImage);
        audioImageView.setFitHeight(50);
        audioImageView.setFitWidth(50);

        Button audioButton = new Button();
        audioButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
        audioButton.setGraphic(audioImageView);
        audioButton.setOnAction(e -> playWordTTS());
        textBox.getChildren().add(audioButton);

        Label textLabel = new Label(word.getKana());
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 32pt");
        textBox.getChildren().add(textLabel);

        vbox.getChildren().add(textBox);

        HBox buttonBox1 = new HBox();
        buttonBox1.setAlignment(Pos.CENTER);
        buttonBox1.setSpacing(50);
        buttonBox1.getChildren().addAll(createWordButton(), createWordButton());

        HBox buttonBox2 = new HBox();
        buttonBox2.setAlignment(Pos.CENTER);
        buttonBox2.setSpacing(50);
        buttonBox2.getChildren().addAll(createWordButton(), createWordButton());

        vbox.getChildren().addAll(buttonBox1, buttonBox2);

        playWordTTS();

        return vbox;
    }

    @Override
    public void checkComplete() {
        Controller.getInstance().stopAudio();
        disableButton.set(true);

        if(selectedButton.getText().equals(word.getJapanese())){
            super.finished(true, word.getEnglish());
        } else {
            super.finished(false, word.getEnglish());
        }
    }

    public void playWordTTS(){
        Controller.getInstance().playAudio(word.getTtsPath());
    }

    private void getWrongKanjis() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM KANJI ORDER BY RAND() LIMIT " + (correctKanjis.size()*3);
        wrongKanjis = entityManager.createNativeQuery(sql, Kanji.class).getResultList();
    }

    private ToggleButton createWordButton() {
        ToggleButton toggleButton = new ToggleButton();
        toggleButton.getStyleClass().add("menuButton");
        toggleButton.disableProperty().bind(disableButton);

        if(correctButtonNumber == 0){
            toggleButton.setText(word.getJapanese());
        } else {
            List<Kanji> useKanjis = new ArrayList<>(correctKanjis.size());
            Random rand = new Random();
            for (int i = 0; i < correctKanjis.size(); i++) {
                Kanji kanji = wrongKanjis.get(rand.nextInt(wrongKanjis.size()));
                wrongKanjis.remove(kanji);
                useKanjis.add(kanji);
            }

            String text = word.getJapanese();

            for (int i = 0; i < useKanjis.size(); i++) {
                text = text.replace(correctKanjis.get(i).getSymbol(), useKanjis.get(i).getSymbol());
            }
            toggleButton.setText(text);
        }
        correctButtonNumber--;

        toggleButton.setOnAction(e -> {
            animateButtonClick(toggleButton);

            if (toggleButton.isSelected()) {
                animateButtonClick(toggleButton);
                if (selectedButton != null) {
                    selectedButton.setSelected(false);
                } else {
                    super.activateCheckButton(true);
                }
                selectedButton = toggleButton;
            } else {
                super.activateCheckButton(false);
                selectedButton = null;
            }
        });

        return toggleButton;
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
