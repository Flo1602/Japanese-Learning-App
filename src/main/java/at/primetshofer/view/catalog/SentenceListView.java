package at.primetshofer.view.catalog;

import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SentenceListView extends View{

    private int scrollPage = 0;
    private ChangeListener scrollPageListener;
    private ScrollPane sentenceList;
    private TextField searchField;
    private String search;
    private Word word;

    public SentenceListView(Scene scene) {
        super(scene);
    }

    public SentenceListView(Scene scene, String search, Word word) {
        this.search = search;
        this.scene = scene;
        this.word = word;
        origin = new SimpleObjectProperty<>();
        initView();
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("SentenceListHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        Label searchLabel = new Label(LangController.getText("SearchLabel"));
        searchLabel.getStyleClass().add("normalText");

        searchField = new TextField();
        if(search != null){
            searchField.setText(search);
            searchField.setDisable(true);
        }

        HBox searchBox = new HBox(searchLabel, searchField);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setSpacing(20);

        sentenceList = new ScrollPane();
        sentenceList.setFitToWidth(true);
        populateSentenceList();

        searchField.setOnAction(e -> {
            populateSentenceList();
        });

        VBox center = new VBox(searchBox, sentenceList);
        center.setAlignment(Pos.CENTER);

        Button addButton = new Button(LangController.getText("AddSentenceButton"));
        addButton.getStyleClass().add("wordListButton");
        addButton.setOnAction(event -> {
            CreateEditSentenceWindow window = new CreateEditSentenceWindow(word);
            window.showAndWait();
            populateSentenceList();
        });

        Button importButton = new Button(LangController.getText("ImportButton"));
        importButton.getStyleClass().add("wordListButton");
        importButton.setOnAction(event -> {
            ImportSentencesView importSentencesView = new ImportSentencesView(scene, word);
            importSentencesView.display(this);
        });

        HBox buttons = new HBox(addButton, importButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setSpacing(40);
        buttons.setPadding(new Insets(20, 0, 20, 0));

        if(word == null){
            buttons.setVisible(false);
        }

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(center);
        bp.setBottom(buttons);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private void populateSentenceList(){
        scrollPage = 0;
        String search = searchField.getText();
        if(scrollPageListener != null){
            sentenceList.vvalueProperty().removeListener(scrollPageListener);
        }
        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        EntityManager em = HibernateUtil.getEntityManager();

        List<Sentence> sentences = fetchWords(em, 0, 50, search);
        List<SentenceCard> sentenceCards = new ArrayList<>();

        for (Sentence sentence : sentences) {
            SentenceCard card = new SentenceCard(sentence);
            sentenceCards.add(card);
            vb.getChildren().add(card.getCard());
        }

        List<Sentence> nextNewSentences = fetchWords(em, 5 * (scrollPage+1), 50, search);
        List<Sentence> prevNewSentences = fetchWords(em, 5 * (scrollPage), 50, search);
        AtomicBoolean listsUpdated = new AtomicBoolean(true);

        scrollPageListener = (ChangeListener<Number>) (observableValue, oldValue, newValue) -> {
            if (!listsUpdated.get() || sentences.size() < 50) {
                return;
            }

            if (newValue.doubleValue() > 0.8) {
                scrollPage++;
                listsUpdated.set(false);
                int dif = sentenceCards.size() - nextNewSentences.size();
                for (int i = 0; i < nextNewSentences.size(); i++) {
                    SentenceCard card = sentenceCards.get(i + dif);

                    card.setEnglish(nextNewSentences.get(i).getEnglish());
                    card.setJapanese(nextNewSentences.get(i).getJapanese());
                    card.setId(nextNewSentences.get(i).getId());
                }

                if (!nextNewSentences.isEmpty()) {
                    sentenceList.setVvalue(oldValue.doubleValue() - 0.1 - ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateSentenceLists(em, nextNewSentences, prevNewSentences, listsUpdated, search);
                } else {
                    scrollPage--;
                }
            }
            if (newValue.doubleValue() < 0.2 && scrollPage > 0) {
                scrollPage--;
                listsUpdated.set(false);
                for (int i = 0; i < prevNewSentences.size(); i++) {
                    SentenceCard card = sentenceCards.get(i);

                    card.setEnglish(prevNewSentences.get(i).getEnglish());
                    card.setJapanese(nextNewSentences.get(i).getJapanese());
                    card.setId(prevNewSentences.get(i).getId());
                }

                if (!prevNewSentences.isEmpty()) {
                    sentenceList.setVvalue(oldValue.doubleValue() + 0.1 + ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateSentenceLists(em, nextNewSentences, prevNewSentences, listsUpdated, search);
                } else {
                    scrollPage++;
                }
            }
        };

        sentenceList.setVvalue(0);
        sentenceList.setPrefHeight(500);
        sentenceList.setContent(vb);
        sentenceList.vvalueProperty().addListener(scrollPageListener);
    }

    private List<Sentence> fetchWords(EntityManager em, int start, int limit, String searchString) {
        // Base JPQL query
        StringBuilder queryString = new StringBuilder("SELECT s FROM Sentence s LEFT JOIN s.word w");

        // Check if searchString is provided and add conditions to the query
        if (searchString != null && !searchString.trim().isEmpty() && word == null) {
            searchString = searchString.toLowerCase();
            queryString.append(" WHERE LOWER(s.japanese) LIKE \'%" + searchString + "%\'")
                    .append(" OR LOWER(s.english) LIKE \'%" + searchString + "%\'")
                    .append(" OR LOWER(w.japanese) LIKE \'%" + searchString + "%\'")
                    .append(" OR LOWER(w.english) LIKE \'%" + searchString + "%\'");
        } else if(word != null){
            queryString.append(" WHERE s.word.id = " + word.getId());
        }

        // Add sorting and create query
        queryString.append(" ORDER BY s.usedWordCount DESC, s.id ASC");
        TypedQuery<Sentence> query = em.createQuery(queryString.toString(), Sentence.class)
                .setFirstResult(start)
                .setMaxResults(limit);

        return query.getResultList();
    }

    private void updateSentenceLists(EntityManager em, List<Sentence> nextNewSentence, List<Sentence> prevNewSentence, AtomicBoolean listsUpdated, String search) {
        final int scrollPageFinal = scrollPage;
        new Thread(() -> {
            synchronized (nextNewSentence){
                nextNewSentence.clear();
                nextNewSentence.addAll(fetchWords(em, 5 * (scrollPageFinal+1), 50, search));
                if(scrollPageFinal>0){
                    prevNewSentence.clear();
                    prevNewSentence.addAll(fetchWords(em, 5 * (scrollPageFinal-1), 50, search));
                }
                listsUpdated.set(true);
            }
        }).start();
    }

    @Override
    public void popToView() {
        super.popToView();
        populateSentenceList();
    }

    private class SentenceCard{

        private Label japanese;
        private Label english;
        private int id;

        private SentenceCard(Sentence sentence){
            this.japanese = new Label(sentence.getJapanese());
            this.english = new Label(sentence.getEnglish());
            this.id = sentence.getId();
        }

        private StackPane getCard(){
            StackPane card = new StackPane();
            card.setPadding(new Insets(15));
            card.setPrefWidth(900);
            card.setAlignment(Pos.CENTER);

            Pane cardBackground = new Pane();
            cardBackground.setPrefSize(300, 100);
            cardBackground.setStyle("-fx-background-color: #4DA8DA; -fx-background-radius: 25;");

            japanese.setStyle("-fx-font-size: 18pt; -fx-text-fill: white;");
            english.setStyle("-fx-font-size: 14pt; -fx-text-fill: white");
            japanese.setMaxWidth(600);
            english.setMaxWidth(600);

            VBox labels = new VBox(5, japanese, english);
            labels.setAlignment(Pos.CENTER_LEFT);
            labels.setPadding(new Insets(0, 0, 0, 50));

            Image audioImage = new Image("audio.png");

            ImageView audioImageView = new ImageView(audioImage);
            audioImageView.setFitHeight(35);
            audioImageView.setFitWidth(35);

            Button audioButton = new Button();
            audioButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            audioButton.setGraphic(audioImageView);
            audioButton.setOnAction(e ->{

                try{
                    Sentence sentence = HibernateUtil.getEntityManager().find(Sentence.class, id);
                    Media media = new Media(new File(sentence.getTtsPath()).toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } catch (Exception ex){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Audio not found!");
                    alert.showAndWait();
                }
            });

            Image editImage = new Image("edit.png");

            ImageView editImageView = new ImageView(editImage);
            editImageView.setFitHeight(35);
            editImageView.setFitWidth(35);

            Button editButton = new Button();
            editButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            editButton.setGraphic(editImageView);
            editButton.setOnAction(event ->{
                Sentence sentence = HibernateUtil.getEntityManager().find(Sentence.class, id);
                Word word = HibernateUtil.getEntityManager().find(Word.class, sentence.getWord().getId());
                CreateEditSentenceWindow window = new CreateEditSentenceWindow(word);
                window.setSentence(sentence);
                window.showAndWait();
                populateSentenceList();
            });

            HBox controls = new HBox(10, audioButton, editButton);
            controls.setPadding(new Insets(0, 50, 0, 0));
            controls.setSpacing(40);
            controls.setAlignment(Pos.CENTER);

            BorderPane content = new BorderPane();
            content.setLeft(labels);
            content.setRight(controls);
            card.getChildren().addAll(cardBackground, content);

            return card;
        }

        public void setJapanese(String japanese) {
            this.japanese.setText(japanese);
        }

        public void setEnglish(String english) {
            this.english.setText(english);
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
