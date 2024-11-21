package at.primetshofer.view.catalog;

import at.primetshofer.Main;
import at.primetshofer.model.AnkiParser;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WordListView extends View{

    private int scrollPage = 0;
    private ChangeListener scrollPageListener;
    private ScrollPane wordList;
    private TextField searchField;
    private String search;

    public WordListView(Scene scene) {
        super(scene);
    }

    public WordListView(Scene scene, String search) {
        this.search = search;
        this.scene = scene;
        origin = new SimpleObjectProperty<>();
        initView();
    }

    protected void initView(){
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("WordListHeading"));
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

        wordList = new ScrollPane();
        wordList.setFitToWidth(true);
        populateWordList();

        searchField.setOnAction(e -> {
            populateWordList();
        });

        VBox center = new VBox(searchBox, wordList);
        center.setAlignment(Pos.CENTER);

        Button addButton = new Button(LangController.getText("AddWordButton"));
        addButton.getStyleClass().add("wordListButton");
        addButton.setOnAction(event -> {
            CreateEditWordWindow window = new CreateEditWordWindow();
            window.showAndWait();
            populateWordList();
        });

        Button importButton = new Button(LangController.getText("ImportButton"));
        importButton.getStyleClass().add("wordListButton");
        importButton.setOnAction(event ->{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open File");

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Anki File", "*.apkg")
            );

            File selectedFile = fileChooser.showOpenDialog(Main.primaryStage);

            if (selectedFile != null) {
                DoubleProperty progressProperty = new SimpleDoubleProperty(0);

                new Thread(() -> AnkiParser.importAnki(selectedFile.getPath(), progressProperty)).start();

                LoadingView loadingView = new LoadingView(scene);
                loadingView.bindProgress(progressProperty);
                loadingView.display(this);

                progressProperty.addListener((observableValue, oldValue, newValue) -> {
                    if(newValue.doubleValue() >= 1.0) {
                        Platform.runLater(() ->{
                            this.popToView();
                            populateWordList();
                        });
                    }
                });
            }
        });

        HBox buttons = new HBox(addButton, importButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setSpacing(40);
        buttons.setPadding(new Insets(20, 0, 20, 0));

        if(search != null){
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

    private void populateWordList(){
        scrollPage = 0;
        String search = searchField.getText();
        if(scrollPageListener != null){
            wordList.vvalueProperty().removeListener(scrollPageListener);
        }
        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        EntityManager em = HibernateUtil.getEntityManager();

        List<Word> words = fetchWords(em, 0, 50, search);
        List<WordCard> wordCards = new ArrayList<>();

        for (Word word : words) {
            WordCard card = new WordCard(word);
            wordCards.add(card);
            vb.getChildren().add(card.getCard());
        }

        List<Word> nextNewWords = fetchWords(em, 5 * (scrollPage+1), 50, search);
        List<Word> prevNewWords = fetchWords(em, 5 * (scrollPage), 50, search);
        AtomicBoolean listsUpdated = new AtomicBoolean(true);

        scrollPageListener = (ChangeListener<Number>) (observableValue, oldValue, newValue) -> {
            if (!listsUpdated.get() || words.size() < 50) {
                return;
            }

            if (newValue.doubleValue() > 0.8) {
                scrollPage++;
                listsUpdated.set(false);
                int dif = wordCards.size() - nextNewWords.size();
                for (int i = 0; i < nextNewWords.size(); i++) {
                    WordCard card = wordCards.get(i + dif);

                    String japanese = nextNewWords.get(i).getJapanese();
                    String kana = nextNewWords.get(i).getKana();

                    if(!kana.equals(japanese) && !kana.trim().isEmpty()){
                        japanese = japanese + " (" + kana + ")";
                    }

                    card.setEnglish(nextNewWords.get(i).getEnglish());
                    card.setJapanese(japanese);
                    card.setActive(nextNewWords.get(i).isActive());
                    card.setId(nextNewWords.get(i).getId());
                }

                if (nextNewWords.size() != 0) {
                    wordList.setVvalue(oldValue.doubleValue() - 0.1 - ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateWordLists(em, nextNewWords, prevNewWords, listsUpdated, search);
                } else {
                    scrollPage--;
                }
            }
            if (newValue.doubleValue() < 0.2 && scrollPage > 0) {
                scrollPage--;
                listsUpdated.set(false);
                for (int i = 0; i < prevNewWords.size(); i++) {
                    WordCard card = wordCards.get(i);
                    String japanese = prevNewWords.get(i).getJapanese();
                    String kana = prevNewWords.get(i).getKana();

                    if(!kana.equals(japanese) && !kana.trim().isEmpty()){
                        japanese = japanese + " (" + kana + ")";
                    }

                    card.setEnglish(prevNewWords.get(i).getEnglish());
                    card.setJapanese(japanese);
                    card.setActive(prevNewWords.get(i).isActive());
                    card.setId(prevNewWords.get(i).getId());
                }

                if (prevNewWords.size() != 0) {
                    wordList.setVvalue(oldValue.doubleValue() + 0.1 + ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateWordLists(em, nextNewWords, prevNewWords, listsUpdated, search);
                } else {
                    scrollPage++;
                }
            }
        };

        wordList.setVvalue(0);
        wordList.setPrefHeight(500);
        wordList.setContent(vb);
        wordList.vvalueProperty().addListener(scrollPageListener);
    }

    private void updateWordLists(EntityManager em, List<Word> nextNewWords, List<Word> prevNewWords, AtomicBoolean listsUpdated, String search) {
        final int scrollPageFinal = scrollPage;
        new Thread(() -> {
            synchronized (nextNewWords){
                nextNewWords.clear();
                nextNewWords.addAll(fetchWords(em, 5 * (scrollPageFinal+1), 50, search));
                if(scrollPageFinal>0){
                    prevNewWords.clear();
                    prevNewWords.addAll(fetchWords(em, 5 * (scrollPageFinal-1), 50, search));
                }
                listsUpdated.set(true);
            }
        }).start();
    }

    private List<Word> fetchWords(EntityManager em, int start, int limit, String searchString) {
        // Base JPQL query
        StringBuilder queryString = new StringBuilder("SELECT w FROM Word w");

        // Check if searchString is provided and add conditions to the query
        if (searchString != null && !searchString.trim().isEmpty()) {
            searchString = searchString.toLowerCase();
            queryString.append(" WHERE LOWER(w.japanese) LIKE \'%" + searchString + "%\'")
                    .append(" OR LOWER(w.english) LIKE \'%" + searchString + "%\'")
                    .append(" OR LOWER(w.kana) LIKE \'%" + searchString + "%\'");
        }

        // Add sorting and create query
        queryString.append(" ORDER BY w.learned DESC, w.id ASC");
        TypedQuery<Word> query = em.createQuery(queryString.toString(), Word.class)
                .setFirstResult(start)
                .setMaxResults(limit);

        return query.getResultList();
    }

    private class WordCard{

        private Label japanese;
        private Label english;
        private CheckBox active;
        private int id;

        private WordCard(Word word){
            String japanese = word.getJapanese();
            if(word.getKana() != null && !word.getKana().equals(word.getJapanese()) && !word.getKana().isBlank()){
                japanese = japanese + " (" + word.getKana() + ")";
            }

            this.japanese = new Label(japanese);
            this.english = new Label(word.getEnglish());
            this.active = new CheckBox(LangController.getText("ActiveCheckbox"));
            this.active.setSelected(word.isActive());
            this.id = word.getId();
        }

        private StackPane getCard(){
            StackPane card = new StackPane();
            card.setPadding(new Insets(15));
            card.setPrefWidth(900);
            card.setAlignment(Pos.CENTER);

            Pane cardBackground = new Pane();
            cardBackground.setPrefSize(300, 100);
            cardBackground.setStyle("-fx-background-color: #4DA8DA; -fx-background-radius: 25;");

            japanese.getStyleClass().add("normalText");
            japanese.setStyle("-fx-text-fill: white");
            english.setStyle("-fx-font-size: 18pt; -fx-text-fill: white");
            japanese.setMaxWidth(400);
            english.setMaxWidth(400);

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
                    Word word = HibernateUtil.getEntityManager().find(Word.class, id);
                    Media media = new Media(new File(word.getTtsPath()).toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } catch (Exception ex){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Audio not found!");
                    alert.showAndWait();
                }
            });

            Button sentences = new Button(LangController.getText("SentencesButton"));
            sentences.getStyleClass().add("smallButton");
            sentences.setOnAction(e -> {
                Word word = HibernateUtil.getEntityManager().find(Word.class, id);
                SentenceListView sentenceListView = new SentenceListView(scene, word.getJapanese(), word);
                sentenceListView.display(WordListView.this);
            });

            Button questions = new Button(LangController.getText("QuestionsButton"));
            questions.getStyleClass().add("smallButton");
            questions.setOnAction(e -> {
                Word word = HibernateUtil.getEntityManager().find(Word.class, id);
                QuestionListView questionListView = new QuestionListView(scene, word.getJapanese(), word);
                questionListView.display(WordListView.this);
            });

            VBox sentenceAndQuestions = new VBox(10, sentences, questions);
            sentenceAndQuestions.setAlignment(Pos.CENTER);

            Image editImage = new Image("edit.png");

            ImageView editImageView = new ImageView(editImage);
            editImageView.setFitHeight(35);
            editImageView.setFitWidth(35);

            Button editButton = new Button();
            editButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            editButton.setGraphic(editImageView);
            editButton.setOnAction(event ->{
                CreateEditWordWindow window = new CreateEditWordWindow();
                Word word = HibernateUtil.getEntityManager().find(Word.class, id);
                window.setWord(word);
                window.showAndWait();
                populateWordList();
            });

            active.setStyle("-fx-font-size: 16pt");
            active.setOnAction(event ->{
                EntityManager em = HibernateUtil.getEntityManager();
                Word word = em.find(Word.class, id);
                word.setActive(active.isSelected());
                HibernateUtil.startTransaction();
                em.merge(word);
                HibernateUtil.commitTransaction();
            });

            HBox controls = new HBox(sentenceAndQuestions, active, audioButton, editButton);
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

        public void setActive(boolean active) {
            this.active.setSelected(active);
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
