package at.primetshofer.view.catalog;

import at.primetshofer.model.TTS;
import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.entities.SentenceWord;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;

public class CreateEditSentenceWindow extends PopUp{

    private Sentence sentence;
    private Word word;
    private boolean create;
    private ObservableList<SentenceWord> tableData;

    public CreateEditSentenceWindow(Word word) {
        super();
        setTitle("Sentence Editor");
        setStageSize(700, 600);
        this.word = word;
    }

    @Override
    protected void initView() {
        if(sentence == null) {
            sentence = new Sentence();
            create = true;
        }
        Label japaneseLabel = new Label(LangController.getText("JapaneseLabel"));
        japaneseLabel.getStyleClass().add("normalText");

        TextField japaneseField = new TextField(sentence.getJapanese());
        japaneseField.setPrefWidth(400);

        HBox japaneseHbox = new HBox(japaneseLabel, japaneseField);
        japaneseHbox.setAlignment(Pos.CENTER);
        japaneseHbox.setSpacing(50);

        Label englishLabel = new Label(LangController.getText("EnglishLabel"));
        englishLabel.getStyleClass().add("normalText");

        TextField englishField = new TextField(sentence.getEnglish());
        englishField.setPrefWidth(400);

        HBox englishHbox = new HBox(englishLabel, englishField);
        englishHbox.setAlignment(Pos.CENTER);
        englishHbox.setSpacing(50);

        VBox vBox = new VBox(japaneseHbox, englishHbox, getWordTable());
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(20);

        Button save = new Button(LangController.getText("saveButton"));
        save.getStyleClass().add("normalButton");
        save.setOnAction(e -> {
            save.setDisable(true);
            EntityManager em = HibernateUtil.getEntityManager();
            HibernateUtil.startTransaction();

            word = em.find(Word.class, word.getId());

            sentence.setEnglish(englishField.getText());
            sentence.setJapanese(japaneseField.getText());
            sentence.setWord(word);

            if(create) {
                em.persist(sentence);
            } else {
                sentence = em.merge(sentence);
            }

            word.getSentences().add(sentence);

            em.merge(word);

            new Thread(() ->{
                try {
                    String ttsString = sentence.getJapanese();

                    File file = TTS.getTts().synthesizeAudio(ttsString, "audio/sentences/" + sentence.getId() + ".wav");
                    sentence.setTtsPath(file.getAbsolutePath());
                    sentence = em.merge(sentence);

                    HibernateUtil.commitTransaction();
                    HibernateUtil.startTransaction();

                    parseTableData(em);

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        ex.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("TTS API not available!");
                        alert.showAndWait();
                    });
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

            sentence = em.find(Sentence.class, sentence.getId());

            em.remove(sentence);

            HibernateUtil.commitTransaction();

            close();
        });

        HBox buttonHbox = new HBox(save);
        buttonHbox.setSpacing(20);
        buttonHbox.setAlignment(Pos.CENTER);
        buttonHbox.setPadding(new Insets(10, 0, 10, 0));

        if(!create) {
            buttonHbox.getChildren().add(delete);
        }

        bp.setCenter(vBox);
        bp.setBottom(buttonHbox);
    }

    private void parseTableData(EntityManager em) {
        for (SentenceWord sentenceWord : sentence.getSentenceWords()) {
            em.remove(sentenceWord);
        }

        sentence.getSentenceWords().clear();

        int offset = 0;

        for(int i=0; i<tableData.size(); i++) {
            int index = i-offset;

            if(tableData.get(i).getWordPos() != -1){
                tableData.get(i).setSentence(sentence);
                tableData.get(i).setWordPos(index);

                if(tableData.get(i).getWordJapanese() != null &&
                        tableData.get(i).getWordEnglish() != null &&
                        !tableData.get(i).getWordJapanese().isBlank() &&
                        !tableData.get(i).getWordEnglish().isBlank()) {
                    sentence.getSentenceWords().add(tableData.get(i));
                    em.persist(tableData.get(i));
                } else {
                    offset++;
                }
            }
        }
    }

    public TableView<SentenceWord> getWordTable() {
        TableView<SentenceWord> tableView = new TableView<>();

        SentenceWord emptyElement = new SentenceWord();
        emptyElement.setWordPos(-1);

        tableData = FXCollections.observableArrayList(sentence.getSentenceWords());
        tableData.add(emptyElement);


        TableColumn<SentenceWord, String> japaneseColumn = new TableColumn<>(LangController.getText("JapaneseLabel"));
        japaneseColumn.setCellValueFactory(new PropertyValueFactory<>("wordJapanese"));
        japaneseColumn.setPrefWidth(199);
        japaneseColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        japaneseColumn.setOnEditCommit(event -> {
            if(event.getRowValue().getWordPos() == -1){
                SentenceWord sentenceWord = new SentenceWord();
                sentenceWord.setWordJapanese(event.getNewValue());
                tableData.add(tableView.getItems().size()-1, sentenceWord);
            } else {
                SentenceWord sentenceWord = event.getRowValue();
                sentenceWord.setWordJapanese(event.getNewValue());
            }
        });

        TableColumn<SentenceWord, String> englishColumn = new TableColumn<>(LangController.getText("EnglishLabel"));
        englishColumn.setCellValueFactory(new PropertyValueFactory<>("wordEnglish"));
        englishColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        englishColumn.setPrefWidth(199);
        englishColumn.setOnEditCommit(event -> {
            if(event.getRowValue().getWordPos() == -1){
                SentenceWord sentenceWord = new SentenceWord();
                sentenceWord.setWordEnglish(event.getNewValue());
                tableData.add(tableView.getItems().size()-1, sentenceWord);
            } else {
                SentenceWord sentenceWord = event.getRowValue();
                sentenceWord.setWordEnglish(event.getNewValue());
            }
        });

        TableColumn<SentenceWord, String> kanaColumn = new TableColumn<>(LangController.getText("KanaLabel"));
        kanaColumn.setCellValueFactory(new PropertyValueFactory<>("wordKana"));
        kanaColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        kanaColumn.setPrefWidth(199);
        kanaColumn.setOnEditCommit(event -> {
            if(event.getRowValue().getWordPos() == -1){
                SentenceWord sentenceWord = new SentenceWord();
                sentenceWord.setWordKana(event.getNewValue());
                tableData.add(tableView.getItems().size()-1, sentenceWord);
            } else {
                SentenceWord sentenceWord = event.getRowValue();
                sentenceWord.setWordKana(event.getNewValue());
            }
        });

        TableColumn<SentenceWord, Boolean> createWordColumn = new TableColumn<>();

        createWordColumn.setCellValueFactory(
                p -> new SimpleBooleanProperty(p.getValue().getWordPos() != -1)
        );

        createWordColumn.setCellFactory(
                p -> new ButtonCell(tableView)
        );

        tableView.getColumns().addAll(japaneseColumn, kanaColumn, englishColumn);

        if(!create){
            tableView.getColumns().add(createWordColumn);
        }

        tableView.setItems(tableData);
        tableView.setEditable(true);
        tableView.setMaxWidth(600);
        tableView.setStyle("-fx-table-cell-border-color: white;");

        return tableView;
    }

    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
        create = false;

        initView();
    }

    private class ButtonCell extends TableCell<SentenceWord, Boolean> {
        final Button cellButton = new Button(LangController.getText("createWordButton"));

        ButtonCell(final TableView tblView){

            cellButton.setOnAction(t -> {
                int selectedIndex = getTableRow().getIndex();
                SentenceWord sentenceWord = (SentenceWord) tblView.getItems().get(selectedIndex);

                if(sentenceWord.getWordPos() == -1){
                    return;
                }

                CreateEditWordWindow window = new CreateEditWordWindow();
                Word newWord = new Word();
                newWord.setJapaneseIgnoreKanji(sentenceWord.getWordJapanese());
                newWord.setEnglish(sentenceWord.getWordEnglish());
                newWord.setKana(sentenceWord.getWordKana());
                window.setWord(newWord, true);
                window.setStageOwner(CreateEditSentenceWindow.this.getStage());
                window.showAndWait();
            });
        }

        @Override
        protected void updateItem(Boolean t, boolean empty) {
            super.updateItem(t, empty);
            if(!empty){
                setGraphic(cellButton);
            }
        }
    }
}
