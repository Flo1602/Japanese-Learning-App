package at.primetshofer.view.catalog;

import at.primetshofer.model.TTS;
import at.primetshofer.model.entities.Question;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Objects;

public class CreateEditQuestionWindow extends PopUp {

    private static final Logger logger = Logger.getLogger(CreateEditQuestionWindow.class);
    private Question question;
    private Word word;
    private boolean create;
    private ObservableList<Answer> tableData;

    public CreateEditQuestionWindow(Word word) {
        super();
        setTitle(LangController.getText("QuestionEditorTitle"));
        setStageSize(700, 600);
        this.word = word;
    }

    @Override
    protected void initView() {
        if (question == null) {
            question = new Question();
            create = true;
        }
        Label japaneseLabel = new Label(LangController.getText("JapaneseLabel"));
        japaneseLabel.getStyleClass().add("normalText");

        TextArea japaneseField = new TextArea(question.getJapanese());
        japaneseField.setPrefWidth(400);

        HBox japaneseHbox = new HBox(japaneseLabel, japaneseField);
        japaneseHbox.setAlignment(Pos.CENTER);
        japaneseHbox.setSpacing(50);
        japaneseField.setPrefHeight(300);
        japaneseField.setWrapText(true);

        Label questionLabel = new Label(LangController.getText("QuestionLabel"));
        questionLabel.getStyleClass().add("normalText");

        TextField questionField = new TextField(question.getQuestion());
        questionField.setPrefWidth(400);

        HBox questionHbox = new HBox(questionLabel, questionField);
        questionHbox.setAlignment(Pos.CENTER);
        questionHbox.setSpacing(50);

        TableView<Answer> answerTable = getAnswerTable();

        Label correctAnswerLabel = new Label(LangController.getText("CorrectAnswerLabel"));
        correctAnswerLabel.getStyleClass().add("normalText");

        FilteredList<Answer> filteredItems = new FilteredList<>(tableData, item -> item != null && !item.text.isBlank());

        ComboBox<Answer> answerBox = new ComboBox<>(filteredItems);
        answerBox.setMaxWidth(350);
        answerBox.getSelectionModel().select(new Answer(question.getCorrectAnswer()));

        HBox correctANswerHbox = new HBox(correctAnswerLabel, answerBox);
        correctANswerHbox.setAlignment(Pos.CENTER);
        correctANswerHbox.setSpacing(50);

        VBox vBox = new VBox(japaneseHbox, questionHbox, correctANswerHbox, answerTable);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(20);
        vBox.setPadding(new Insets(20, 0, 0, 0));

        Button save = new Button(LangController.getText("saveButton"));
        save.getStyleClass().add("normalButton");
        save.setOnAction(e -> {
            save.setDisable(true);
            EntityManager em = HibernateUtil.getEntityManager();
            HibernateUtil.startTransaction();

            word = em.find(Word.class, word.getId());

            question.setQuestion(questionField.getText());
            question.setJapanese(japaneseField.getText());
            question.setWord(word);

            if (answerBox.getSelectionModel().getSelectedItem() != null) {
                question.setCorrectAnswer(answerBox.getSelectionModel().getSelectedItem().getText());
            } else {
                question.setCorrectAnswer(null);
            }

            if (create) {
                em.persist(question);
            } else {
                question = em.merge(question);
            }

            word.getQuestions().add(question);

            em.merge(word);

            new Thread(() -> {
                try {
                    String ttsString = question.getJapanese();

                    File file = TTS.getTts().synthesizeAudio(ttsString, "audio/questions/" + question.getId() + ".wav", 0.8);
                    question.setTtsPath(file.getPath());

                    parseTableData(em);

                    question = em.merge(question);
                } catch (Exception ex) {
                    logger.error("Failed to create / save TTS for question: '" + this.question.getQuestion() + "'");

                    ViewUtils.showAlert(Alert.AlertType.ERROR,
                            LangController.getText("TTSNotAvailableError"),
                            LangController.getText("ErrorText"));
                } finally {
                    HibernateUtil.commitTransaction();

                    Platform.runLater(this::close);
                }
            }).start();
        });

        Button delete = new Button(LangController.getText("DeleteButton"));
        delete.getStyleClass().add("normalButton");
        delete.setOnAction(e -> {
            EntityManager em = HibernateUtil.getEntityManager();
            HibernateUtil.startTransaction();

            question = em.find(Question.class, question.getId());

            em.remove(question);

            HibernateUtil.commitTransaction();

            close();
        });

        HBox buttonHbox = new HBox(save);
        buttonHbox.setSpacing(20);
        buttonHbox.setAlignment(Pos.CENTER);
        buttonHbox.setPadding(new Insets(10, 0, 10, 0));

        if (!create) {
            buttonHbox.getChildren().add(delete);
        }

        bp.setCenter(vBox);
        bp.setBottom(buttonHbox);
    }

    private void parseTableData(EntityManager em) {
        question.getAnswers().clear();

        int offset = 0;

        for (int i = 0; i < tableData.size(); i++) {
            int index = i - offset;

            if (!tableData.get(i).isEmpty()) {
                String answer = tableData.get(i).getText();

                if (!answer.isBlank()) {
                    question.getAnswers().add(answer);
                } else {
                    offset++;
                }
            }
        }
    }

    public TableView<Answer> getAnswerTable() {
        TableView<Answer> tableView = new TableView<>();

        Answer emptyElement = new Answer("");
        emptyElement.setEmpty(true);

        tableData = FXCollections.observableArrayList();
        for (String answer : question.getAnswers()) {
            Answer answerElement = new Answer(answer);
            tableData.add(answerElement);
        }
        tableData.add(emptyElement);

        TableColumn<Answer, String> answerColumn = new TableColumn<>(LangController.getText("AnswerLabel"));
        answerColumn.setCellValueFactory(new PropertyValueFactory<>("text"));
        answerColumn.setPrefWidth(598);
        answerColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        answerColumn.setOnEditCommit(event -> {
            if (event.getRowValue().isEmpty() && !event.getNewValue().isBlank()) {
                Answer answer = new Answer("");
                answer.setText(event.getNewValue());
                tableData.add(tableView.getItems().size() - 1, answer);
            } else {
                if (event.getNewValue().isBlank()) {
                    tableData.remove(event.getRowValue());
                } else {
                    Answer answer = event.getRowValue();
                    answer.setText(event.getNewValue());
                }
            }
        });

        tableView.getColumns().addAll(answerColumn);
        tableView.setItems(tableData);
        tableView.setEditable(true);
        tableView.setMaxWidth(600);
        tableView.setStyle("-fx-table-cell-border-color: white;");

        return tableView;
    }

    public void setQuestion(Question question) {
        this.question = question;
        create = false;

        initView();
    }

    public static class Answer {
        private String text;
        private boolean empty = false;

        public Answer(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isEmpty() {
            return empty;
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Answer answer = (Answer) o;
            return Objects.equals(text, answer.text);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(text);
        }
    }
}
