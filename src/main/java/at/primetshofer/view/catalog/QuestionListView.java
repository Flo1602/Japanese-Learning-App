package at.primetshofer.view.catalog;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Question;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuestionListView extends View {

    private int scrollPage = 0;
    private ChangeListener scrollPageListener;
    private ScrollPane questionList;
    private TextField searchField;
    private String search;
    private Word word;

    public QuestionListView(Scene scene) {
        super(scene);
    }

    public QuestionListView(Scene scene, String search, Word word) {
        this.search = search;
        this.scene = scene;
        this.word = word;
        origin = new SimpleObjectProperty<>();
        initView();
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("QuestionListHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        Label searchLabel = new Label(LangController.getText("SearchLabel"));
        searchLabel.getStyleClass().add("normalText");

        searchField = new TextField();
        if (search != null) {
            searchField.setText(search);
            searchField.setDisable(true);
        }

        HBox searchBox = new HBox(searchLabel, searchField);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setSpacing(20);

        questionList = new ScrollPane();
        questionList.setFitToWidth(true);
        populateQuestionList();

        searchField.setOnAction(e -> {
            populateQuestionList();
        });

        VBox center = new VBox(searchBox, questionList);
        center.setAlignment(Pos.CENTER);

        Button addButton = new Button(LangController.getText("AddQuestionButton"));
        addButton.getStyleClass().add("wordListButton");
        addButton.setOnAction(event -> {
            CreateEditQuestionWindow window = new CreateEditQuestionWindow(word);
            window.showAndWait();
            populateQuestionList();
        });

        Button importButton = new Button(LangController.getText("ImportButton"));
        importButton.getStyleClass().add("wordListButton");
        importButton.setOnAction(event -> {
            ImportQuestionsView importQuestionsView = new ImportQuestionsView(scene, word);
            importQuestionsView.display(this);
        });

        HBox buttons = new HBox(addButton, importButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setSpacing(40);
        buttons.setPadding(new Insets(20, 0, 20, 0));

        if (word == null) {
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

    private void populateQuestionList() {
        scrollPage = 0;
        String search = searchField.getText();
        if (scrollPageListener != null) {
            questionList.vvalueProperty().removeListener(scrollPageListener);
        }
        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        EntityManager em = HibernateUtil.getEntityManager();

        List<Question> questions = fetchWords(em, 0, 50, search);
        List<QuestionCard> questionCards = new ArrayList<>();

        for (Question question : questions) {
            QuestionCard card = new QuestionCard(question);
            questionCards.add(card);
            vb.getChildren().add(card.getCard());
        }

        List<Question> nextNewQuestions = fetchWords(em, 5 * (scrollPage + 1), 50, search);
        List<Question> prevNewQuestions = fetchWords(em, 5 * (scrollPage), 50, search);
        AtomicBoolean listsUpdated = new AtomicBoolean(true);

        scrollPageListener = (ChangeListener<Number>) (observableValue, oldValue, newValue) -> {
            if (!listsUpdated.get() || questions.size() < 50) {
                return;
            }

            if (newValue.doubleValue() > 0.8) {
                scrollPage++;
                listsUpdated.set(false);
                int dif = questionCards.size() - nextNewQuestions.size();
                for (int i = 0; i < nextNewQuestions.size(); i++) {
                    QuestionCard card = questionCards.get(i + dif);

                    card.setQuestion(nextNewQuestions.get(i).getQuestion());
                    card.setJapanese(nextNewQuestions.get(i).getJapanese());
                    card.setId(nextNewQuestions.get(i).getId());
                }

                if (!nextNewQuestions.isEmpty()) {
                    questionList.setVvalue(oldValue.doubleValue() - 0.1 - ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateQuestionLists(em, nextNewQuestions, prevNewQuestions, listsUpdated, search);
                } else {
                    scrollPage--;
                }
            }
            if (newValue.doubleValue() < 0.2 && scrollPage > 0) {
                scrollPage--;
                listsUpdated.set(false);
                for (int i = 0; i < prevNewQuestions.size(); i++) {
                    QuestionCard card = questionCards.get(i);

                    card.setQuestion(prevNewQuestions.get(i).getQuestion());
                    card.setJapanese(nextNewQuestions.get(i).getJapanese());
                    card.setId(prevNewQuestions.get(i).getId());
                }

                if (!prevNewQuestions.isEmpty()) {
                    questionList.setVvalue(oldValue.doubleValue() + 0.1 + ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateQuestionLists(em, nextNewQuestions, prevNewQuestions, listsUpdated, search);
                } else {
                    scrollPage++;
                }
            }
        };

        questionList.setVvalue(0);
        questionList.setPrefHeight(500);
        questionList.setContent(vb);
        questionList.vvalueProperty().addListener(scrollPageListener);
    }

    private List<Question> fetchWords(EntityManager em, int start, int limit, String searchString) {
        // Base JPQL query
        StringBuilder queryString = new StringBuilder("SELECT q FROM Question q LEFT JOIN q.word w");

        // Check if searchString is provided and add conditions to the query
        if (searchString != null && !searchString.trim().isEmpty()) {
            searchString = searchString.toLowerCase();
            queryString.append(" WHERE LOWER(q.japanese) LIKE '%" + searchString + "%'")
                    .append(" OR LOWER(q.question) LIKE '%" + searchString + "%'")
                    .append(" OR LOWER(w.japanese) LIKE '%" + searchString + "%'")
                    .append(" OR LOWER(w.english) LIKE '%" + searchString + "%'");
        } else if (word != null) {
            queryString.append(" WHERE q.word.id = " + word.getId());
        }

        // Add sorting and create query
        queryString.append(" ORDER BY q.usedWordCount DESC, q.id ASC");
        TypedQuery<Question> query = em.createQuery(queryString.toString(), Question.class)
                .setFirstResult(start)
                .setMaxResults(limit);

        return query.getResultList();
    }

    private void updateQuestionLists(EntityManager em, List<Question> nextNewQuestions, List<Question> prevNewQuestions, AtomicBoolean listsUpdated, String search) {
        final int scrollPageFinal = scrollPage;
        new Thread(() -> {
            synchronized (nextNewQuestions) {
                nextNewQuestions.clear();
                nextNewQuestions.addAll(fetchWords(em, 5 * (scrollPageFinal + 1), 50, search));
                if (scrollPageFinal > 0) {
                    prevNewQuestions.clear();
                    prevNewQuestions.addAll(fetchWords(em, 5 * (scrollPageFinal - 1), 50, search));
                }
                listsUpdated.set(true);
            }
        }).start();
    }

    @Override
    public void popToView() {
        super.popToView();
        populateQuestionList();
    }

    private class QuestionCard {

        private final Label japanese;
        private final Label question;
        private int id;

        private QuestionCard(Question question) {
            this.japanese = new Label(question.getJapanese());
            this.question = new Label(question.getQuestion());
            this.id = question.getId();
        }

        private StackPane getCard() {
            StackPane card = new StackPane();
            card.setPadding(new Insets(15));
            card.setPrefWidth(900);
            card.setAlignment(Pos.CENTER);

            Pane cardBackground = new Pane();
            cardBackground.setPrefSize(300, 100);
            cardBackground.setStyle("-fx-background-color: #4DA8DA; -fx-background-radius: 25;");

            japanese.setStyle("-fx-font-size: 18pt; -fx-text-fill: white;");
            question.setStyle("-fx-font-size: 14pt; -fx-text-fill: white");
            japanese.setMaxWidth(600);
            question.setMaxWidth(600);

            VBox labels = new VBox(5, japanese, question);
            labels.setAlignment(Pos.CENTER_LEFT);
            labels.setPadding(new Insets(0, 0, 0, 50));

            Image audioImage = new Image("audio.png");

            ImageView audioImageView = new ImageView(audioImage);
            audioImageView.setFitHeight(35);
            audioImageView.setFitWidth(35);

            Button audioButton = new Button();
            audioButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            audioButton.setGraphic(audioImageView);
            audioButton.setOnAction(e -> {
                Question question = HibernateUtil.getEntityManager().find(Question.class, id);
                Controller.getInstance().playAudio(question.getTtsPath());
            });

            Image editImage = new Image("edit.png");

            ImageView editImageView = new ImageView(editImage);
            editImageView.setFitHeight(35);
            editImageView.setFitWidth(35);

            Button editButton = new Button();
            editButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt; -fx-background-color: transparent;");
            editButton.setGraphic(editImageView);
            editButton.setOnAction(event -> {
                Question question = HibernateUtil.getEntityManager().find(Question.class, id);
                Word word = HibernateUtil.getEntityManager().find(Word.class, question.getWord().getId());
                CreateEditQuestionWindow window = new CreateEditQuestionWindow(word);
                window.setQuestion(question);
                window.showAndWait();
                populateQuestionList();
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

        public void setQuestion(String question) {
            this.question.setText(question);
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
