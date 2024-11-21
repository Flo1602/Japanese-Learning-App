package at.primetshofer.view.learning.learnViews.sentenceLearnViews;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.entities.SentenceWord;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import at.primetshofer.view.learning.learnViews.LearnView;
import jakarta.persistence.EntityManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SentenceLearnView extends LearnView {

    private String toTranslate;
    private String translation;
    private String solution;
    private int sentenceId;
    private List<SentenceWord> sentenceWords;
    private ArrayList<String> words;
    private Set<String> synonyms;
    private String ttsPath;
    private Map<String, SentenceWord> toTranslateParts;
    private HBox innerTranslationBox;
    private HBox innerTranslationBox2;
    private HBox innerWordBox;
    private HBox innerWordBox2;
    private int maxWidth = 950;
    private Pane animationPane;
    private AtomicBoolean reshuffleAllowed;
    private BooleanProperty disableButtons;
    private StringBuilder userTranslation;

    public SentenceLearnView(LearnSessionManager learnSessionManager, int sentenceId) {
        super(learnSessionManager, true);
        super.activateCheckButton(true);
        reshuffleAllowed = new AtomicBoolean(true);
        disableButtons = new SimpleBooleanProperty(false);
        this.sentenceId = sentenceId;
    }

    @Override
    public Pane initView() {
        Collections.shuffle(words);

        if(sentenceWords != null && !sentenceWords.isEmpty() && toTranslate != null){
            Map<String, SentenceWord> sentenceWordStrings = new LinkedHashMap<>();
            for (SentenceWord sentenceWord : sentenceWords) {
                if(sentenceWord.getWordJapanese() != null && !sentenceWord.getWordJapanese().isBlank()){
                    sentenceWordStrings.put(sentenceWord.getWordJapanese(), sentenceWord);
                }
                if(sentenceWord.getWordEnglish() != null && !sentenceWord.getWordEnglish().isBlank()){
                    sentenceWordStrings.put(sentenceWord.getWordEnglish(), sentenceWord);
                }
            }
            List<String> stringParts = ViewUtils.splitByDelimiters(toTranslate, sentenceWordStrings.keySet());
            toTranslateParts = new LinkedHashMap<>();

            for (String stringPart : stringParts) {
                toTranslateParts.put(stringPart, sentenceWordStrings.getOrDefault(stringPart, null));
            }
        }

        StackPane pane = new StackPane();
        animationPane = new Pane();
        animationPane.setMouseTransparent(true);

        HBox textBox = new HBox();
        textBox.setSpacing(10);
        textBox.setAlignment(Pos.CENTER);

        if(ttsPath != null) {
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

        if(toTranslate != null) {
            if(toTranslateParts != null && !toTranslateParts.isEmpty()){
                HBox textLabels = new HBox();
                textLabels.setAlignment(Pos.CENTER);
                for (Map.Entry<String, SentenceWord> toTranslatePart : toTranslateParts.entrySet()) {
                    Label textLabel = new Label(toTranslatePart.getKey());
                    textLabel.setStyle("-fx-font-size: 20pt");
                    textLabels.getChildren().add(textLabel);

                    if(toTranslatePart.getValue() != null){
                        Tooltip tooltip = new Tooltip(LangController.getText("EnglishLabel") + " " + toTranslatePart.getValue().getWordEnglish() +
                                "\n" + LangController.getText("JapaneseLabel") + " " + toTranslatePart.getValue().getWordJapanese() +
                                "\n" + LangController.getText("KanaLabel") + " " + toTranslatePart.getValue().getWordKana());
                        tooltip.setStyle("-fx-font-size: 16pt");
                        textLabel.setUnderline(true);
                        Tooltip.install(textLabel, tooltip);
                    }
                }
                textBox.getChildren().add(textLabels);
            } else {
                Label textLabel = new Label(toTranslate);
                textLabel.setWrapText(true);
                textLabel.setStyle("-fx-font-size: 20pt");
                textBox.getChildren().add(textLabel);
                textBox.setSpacing(2);
            }
        }

        VBox translationBox = new VBox();
        translationBox.setSpacing(10);
        translationBox.setFillWidth(false);
        translationBox.setStyle("-fx-border-color: #a6a6a6; -fx-padding: 10; -fx-border-width: 1; -fx-border-radius: 10;");
        translationBox.setPrefWidth(1000);
        innerTranslationBox = new HBox();
        innerTranslationBox.setFillHeight(false);
        innerTranslationBox.setSpacing(10);
        Line line = new Line();
        line.startYProperty().bind(innerTranslationBox.heightProperty());
        line.endXProperty().bind(translationBox.widthProperty().subtract(25));
        line.endYProperty().bind(innerTranslationBox.heightProperty());
        line.setStroke(Color.rgb(166, 166, 166));
        innerTranslationBox2 = new HBox();
        innerTranslationBox2.setFillHeight(false);
        innerTranslationBox2.setSpacing(10);
        translationBox.getChildren().addAll(innerTranslationBox, line, innerTranslationBox2);

        VBox wordBox = new VBox();
        wordBox.setSpacing(10);
        wordBox.setFillWidth(false);
        wordBox.setStyle("-fx-border-color: #a6a6a6; -fx-padding: 10; -fx-border-width: 1; -fx-border-radius: 10;");
        wordBox.setPrefWidth(1000);
        innerWordBox = new HBox();
        innerWordBox.setFillHeight(false);
        innerWordBox.setSpacing(10);
        Line line2 = new Line();
        line2.startYProperty().bind(innerTranslationBox.heightProperty());
        line2.endXProperty().bind(translationBox.widthProperty().subtract(25));
        line2.endYProperty().bind(innerTranslationBox.heightProperty());
        line2.setStroke(Color.rgb(166, 166, 166));
        innerWordBox2 = new HBox();
        innerWordBox2.setFillHeight(false);
        innerWordBox2.setSpacing(10);
        wordBox.getChildren().addAll(innerWordBox, line2, innerWordBox2);
        innerTranslationBox.prefHeightProperty().bind(innerWordBox.heightProperty());
        innerWordBox2.prefHeightProperty().bind(innerWordBox.heightProperty());
        innerTranslationBox2.prefHeightProperty().bind(innerWordBox.heightProperty());

        AtomicInteger buttonCntr = new AtomicInteger();

        for (String word : words) {
            buttonCntr.getAndIncrement();
            Button button = new Button(word);
            button.disableProperty().bind(disableButtons);
            button.setUserData(true);
            button.setStyle("-fx-font-size: 14pt");
            button.widthProperty().addListener((observableValue, oldValue, newValue) -> {
                buttonCntr.getAndDecrement();

                if(oldValue.doubleValue() == 0.0 && newValue.doubleValue() > 0.0 && wordBox.getWidth() > 900 && buttonCntr.get() == 0){
                    reshuffleButtons(innerWordBox, innerWordBox2, true);
                    reshuffleButtons(innerWordBox, innerWordBox2, true);
                }

            });

            HBox targetHBox = getHBoxWithSpace(innerWordBox, innerWordBox2, button, false);
            targetHBox.getChildren().add(button);

            button.setOnAction(e -> {
                if(innerWordBox.getMinHeight() <= 0){
                    innerWordBox.setMinHeight(innerWordBox.getHeight());
                }

                wordButtonAction(button, targetHBox, animationPane);
            });
        }

        VBox vbox = new VBox(50, textBox, translationBox, wordBox);
        vbox.setAlignment(Pos.CENTER);
        vbox.setFillWidth(false);

        pane.getChildren().addAll(vbox, animationPane);

        return pane;
    }

    private void reshuffleButtons(HBox hBox1, HBox hBox2, boolean startup){
        if(!reshuffleAllowed.get()){
            return;
        }
        List<Node> list1 = new ArrayList<>(hBox1.getChildren().stream().toList());
        List<Node> list2 = new ArrayList<>(hBox2.getChildren().stream().toList());

        hBox1.getChildren().clear();

        for (Node node : list1) {
            Button button = (Button) node;
            HBox hBox = getHBoxWithSpace(hBox1, hBox2, button, true);
            hBox.getChildren().add(button);

            if(hBox == hBox2){
                button.setOnAction( e -> wordButtonAction(button, hBox2, animationPane));
            }
        }

        for (Node node : list2) {
            Button button = (Button) node;
            HBox hBox = getHBoxWithSpace(hBox1, hBox2, button, true);

            if(hBox == hBox1){
                if(startup){
                    hBox2.getChildren().remove(node);
                    hBox1.getChildren().add(node);
                } else {
                    reshuffleAllowed.set(false);
                    removeWithAnimation(button, hBox2);
                    moveElement(button, hBox2, hBox1, animationPane).addListener((observableValue, oldValue, newValue) -> {
                        if(!newValue){
                            return;
                        }
                        button.setOnAction( e -> wordButtonAction(button, hBox1, animationPane));
                        reshuffleAllowed.set(true);
                        if(checkReshuffleNeeded(hBox1, hBox2)){
                            reshuffleButtons(hBox1, hBox2, false);
                        }
                    });
                    return;
                }
                button.setOnAction( e -> wordButtonAction(button, hBox1, animationPane));
            }
        }
    }

    private HBox getHBoxWithSpace(HBox hBox1, HBox hBox2, Button newElement, boolean reshuffle) {
        if(!reshuffle && !hBox2.getChildren().isEmpty()){
            return hBox2;
        }

        double width = 0.0;

        for (Node child : hBox1.getChildren()) {
            Button button = (Button) child;

            if(button.getWidth() <= 0.0){
                Text text = new Text(button.getText());
                text.setFont(button.getFont());

                double textWidth = text.getBoundsInLocal().getWidth();
                double buttonWidth = textWidth + 50;

                width += buttonWidth;
            }

            width += button.getWidth();
            width += hBox1.getSpacing();
        }

        if((width + newElement.getWidth()) > maxWidth){
            return hBox2;
        }

        return hBox1;
    }

    private boolean checkReshuffleNeeded(HBox hBox1, HBox hBox2){
        if(hBox2.getChildren().isEmpty()){
            return false;
        }

        double width = 0.0;

        for (Node child : hBox1.getChildren()) {
            Button button = (Button) child;
            width += button.getWidth();
            width += hBox1.getSpacing();
        }

        Button firstButton = (Button) hBox2.getChildren().getFirst();

        if((width + firstButton.getWidth()) < maxWidth){
            return true;
        }
        return false;
    }

    private void wordButtonAction(Button button, HBox sourceBox, Pane animationPane) {
        HBox targetBox;
        if((boolean) button.getUserData()){
            button.setUserData(false);
            targetBox = getHBoxWithSpace(this.innerTranslationBox, this.innerTranslationBox2, button, false);
        } else {
            button.setUserData(true);
            targetBox = getHBoxWithSpace(this.innerWordBox, this.innerWordBox2, button, false);
        }

        removeWithAnimation(button, sourceBox);
        moveElement(button, sourceBox, targetBox, animationPane);
    }

    public void playSentenceTTS() {
        if(ttsPath == null){
            return;
        }
        Controller.getInstance().playAudio(ttsPath);
    }

    @Override
    public void checkComplete() {
        disableButtons.set(true);
        userTranslation = new StringBuilder();

        for (Node child : innerTranslationBox.getChildren()) {
            Button button = (Button) child;
            userTranslation.append(button.getText());
        }

        for (Node child : innerTranslationBox2.getChildren()) {
            Button button = (Button) child;
            userTranslation.append(button.getText());
        }

        if(userTranslation.toString().equals(translation) || (synonyms != null && synonyms.contains(userTranslation.toString()))){
            if(toTranslate == null){
                finished(true, solution);
            } else {
                finished(true);
            }

        } else {
            finished(false, LangController.getText("CorrectAnswer") + " " + solution);
        }
    }

    public void removeWithAnimation(Node nodeToRemove, HBox hbox) {
        int indexToRemove = hbox.getChildren().indexOf(nodeToRemove);

        if (indexToRemove < 0) {
            return;
        }
        if(indexToRemove >= hbox.getChildren().size()-1){
            removeFromBox(nodeToRemove, hbox);
            return;
        }

        Timeline timeline = new Timeline();

        nodeToRemove.setVisible(false);

        for (int i = indexToRemove + 1; i < hbox.getChildren().size(); i++) {
            Node child = hbox.getChildren().get(i);

            double shiftAmount = nodeToRemove.getLayoutBounds().getWidth() + hbox.getSpacing();

            KeyValue keyValue = new KeyValue(child.translateXProperty(), -shiftAmount);
            KeyFrame keyFrame = new KeyFrame(Duration.millis(200), keyValue);
            timeline.getKeyFrames().add(keyFrame);
        }

        timeline.setOnFinished(event -> {
            removeFromBox(nodeToRemove, hbox);
        });

        timeline.setDelay(Duration.millis(100));
        timeline.play();
    }

    private void removeFromBox(Node nodeToRemove, HBox hbox) {
        hbox.getChildren().remove(nodeToRemove);

        for (Node child : hbox.getChildren()) {
            child.setTranslateX(0);
        }

        Platform.runLater(() -> {
            if(checkReshuffleNeeded(this.innerWordBox, this.innerWordBox2)){
                reshuffleButtons(this.innerWordBox, this.innerWordBox2, false);
            }

            if(checkReshuffleNeeded(this.innerTranslationBox, this.innerTranslationBox2)){
                reshuffleButtons(this.innerTranslationBox, this.innerTranslationBox2, false);
            }
        });
    }

    private BooleanProperty moveElement(Button element, HBox fromContainer, HBox toContainer, Pane overlayPane) {
        double startX = fromContainer.getBoundsInParent().getMinX() + element.getBoundsInParent().getMinX() + fromContainer.getParent().getBoundsInParent().getMinX();
        double startY = fromContainer.getBoundsInParent().getMinY() + element.getBoundsInParent().getMinY() + fromContainer.getParent().getBoundsInParent().getMinY();

        Button newButton = new Button(element.getText());
        newButton.disableProperty().bind(disableButtons);
        newButton.setUserData(element.getUserData());
        newButton.setStyle(element.getStyle());
        newButton.setOnAction( e -> wordButtonAction(newButton, toContainer, overlayPane));

        overlayPane.getChildren().add(newButton);
        newButton.setTranslateX(startX);
        newButton.setTranslateY(startY);

        double targetX = toContainer.getBoundsInLocal().getWidth() + toContainer.getParent().getBoundsInParent().getMinX();
        double targetY = toContainer.localToParent(toContainer.getBoundsInLocal()).getMinY() + toContainer.getParent().getBoundsInParent().getMinY();

        targetX += toContainer.getPadding().getRight();
        targetY += toContainer.getPadding().getTop();

        targetX += toContainer.getSpacing();

        BooleanProperty animFinished = new SimpleBooleanProperty(false);

        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.4), newButton);
        transition.setToX(targetX);
        transition.setToY(targetY);
        transition.setOnFinished(event -> {
            overlayPane.getChildren().remove(newButton);
            toContainer.getChildren().add(newButton);
            newButton.setTranslateX(0);
            newButton.setTranslateY(0);
            animFinished.set(true);
        });
        transition.play();

        return animFinished;
    }

    protected void setToTranslate(String toTranslate) {
        this.toTranslate = toTranslate;
    }

    protected void setTranslation(String translation) {
        this.translation = translation;
    }

    protected void setWords(ArrayList<String> words) {
        this.words = words;
    }

    protected void setTtsPath(String ttsPath) {
        this.ttsPath = ttsPath;
    }

    public void setSentenceWords(List<SentenceWord> sentenceWords) {
        this.sentenceWords = sentenceWords;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public void setSynonyms(Set<String> synonyms) {
        this.synonyms = synonyms;
    }

    @Override
    public void correctnessOverwritten() {
        HibernateUtil.startTransaction();

        EntityManager em = HibernateUtil.getEntityManager();
        Sentence sentence = em.find(Sentence.class, sentenceId);

        sentence.getSynonyms().add(String.valueOf(userTranslation));

        em.merge(sentence);

        HibernateUtil.commitTransaction();
    }
}
