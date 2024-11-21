package at.primetshofer.view.catalog;

import at.primetshofer.model.TTS;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;

public class CreateEditWordWindow extends PopUp {

    private Word word;
    private boolean create;

    public CreateEditWordWindow() {
        super();

        setTitle("Word Editor");
    }

    @Override
    protected void initView() {
        if(word == null) {
            word = new Word();
            create = true;
        }
        Label japaneseLabel = new Label(LangController.getText("JapaneseLabel"));
        japaneseLabel.getStyleClass().add("normalText");

        TextField japaneseField = new TextField(word.getJapanese());

        HBox japaneseHbox = new HBox(japaneseLabel, japaneseField);
        japaneseHbox.setAlignment(Pos.CENTER);
        japaneseHbox.setSpacing(50);

        Label kanaLabel = new Label(LangController.getText("KanaLabel"));
        kanaLabel.getStyleClass().add("normalText");

        TextField kanaField = new TextField(word.getKana());

        HBox kanaHbox = new HBox(kanaLabel, kanaField);
        kanaHbox.setAlignment(Pos.CENTER);
        kanaHbox.setSpacing(50);

        Label englishLabel = new Label(LangController.getText("EnglishLabel"));
        englishLabel.getStyleClass().add("normalText");

        TextField englishField = new TextField(word.getEnglish());

        HBox englishHbox = new HBox(englishLabel, englishField);
        englishHbox.setAlignment(Pos.CENTER);
        englishHbox.setSpacing(50);

        CheckBox active = new CheckBox(LangController.getText("ActiveCheckbox"));
        active.setStyle("-fx-font-size: 22pt");
        active.setSelected(word.isActive());

        VBox vBox = new VBox(japaneseHbox, kanaHbox, englishHbox, active);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(20);

        Button save = new Button(LangController.getText("saveButton"));
        save.getStyleClass().add("normalButton");
        save.setOnAction(e -> {
            save.setDisable(true);
            EntityManager em = HibernateUtil.getEntityManager();
            HibernateUtil.startTransaction();

            word.setKana(kanaField.getText());
            word.setEnglish(englishField.getText());
            word.setActive(active.isSelected());

            word = em.merge(word);

            word.setJapanese(japaneseField.getText());

            new Thread(() ->{
                try {
                    String ttsString = (word.getKana() == null)? word.getJapanese() : word.getKana();

                    File file = TTS.getTts().synthesizeAudio(ttsString, "audio/words/" + word.getId() + ".wav");
                    word.setTtsPath(file.getAbsolutePath());
                    em.merge(word);

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

            word = em.find(Word.class, word.getId());
            for (Kanji kanji : word.getKanjis()) {
                kanji.getWords().remove(word);
                em.merge(kanji);
            }

            em.remove(word);

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

    public void setWord(Word word) {
        this.word = word;
        create = false;

        initView();
    }

    public void setWord(Word word, boolean create) {
        this.word = word;
        this.create = create;

        initView();
    }
}
