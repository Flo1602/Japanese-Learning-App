package at.primetshofer.view.catalog;

import at.primetshofer.model.Controller;
import at.primetshofer.model.TTS;
import at.primetshofer.model.entities.Question;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImportQuestionsView extends View {

    private static final Logger logger = Logger.getLogger(ImportQuestionsView.class);
    private Word word;
    private final Controller controller;
    private List<Word> words;
    private TextArea jsonInput;

    private Button submit;
    private Label headline;

    public ImportQuestionsView(Scene scene, Word word) {
        super(scene);
        this.word = word;
        this.controller = Controller.getInstance();
        headline.setText(LangController.getText("ImportQuestionsHeadline") + " " + word.getJapanese());
    }

    public ImportQuestionsView(Scene scene, List<Word> words) {
        super(scene);
        this.words = words;
        this.word = words.getFirst();
        words.remove(word);
        this.controller = Controller.getInstance();
        headline.setText(LangController.getText("ImportQuestionsHeadline") + " " + word.getJapanese());
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        headline = new Label();
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        Button copyPrompt = new Button(LangController.getText("CopyPromptButton"));
        copyPrompt.getStyleClass().add("wordListButton");
        copyPrompt.setOnAction(e -> {
            controller.copyToClipboard("Focus word: \"" + word.getJapanese() + "\"");
        });

        Button copyFile = new Button(LangController.getText("CopyFileButton"));
        copyFile.getStyleClass().add("wordListButton");
        copyFile.setOnAction(e -> {
            new Thread(() -> {
                try {
                    controller.copyToClipboard(controller.getWordListCSV());

                    Platform.runLater(() -> {
                        copyFile.setText(LangController.getText("CopiedConfirmation"));
                        copyFile.setTextFill(Color.GREEN);
                        copyFile.setDisable(true);

                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);

                                Platform.runLater(() -> {
                                    copyFile.setText(LangController.getText("CopyFileButton"));
                                    copyFile.setTextFill(copyPrompt.getTextFill());
                                    copyFile.setDisable(false);
                                });
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }).start();
                    });
                } catch (IOException ex) {
                    logger.error("Error while creating CSV", ex);

                    ViewUtils.showAlert(Alert.AlertType.ERROR,
                            LangController.getText("CSVCreationError"),
                            LangController.getText("ErrorText"));
                }
            }).start();
        });

        HBox copyButtons = new HBox(20, copyPrompt, copyFile);
        copyButtons.setAlignment(Pos.CENTER);

        jsonInput = new TextArea();
        jsonInput.setPrefWidth(600);
        jsonInput.setPrefHeight(500);
        jsonInput.setWrapText(false);
        jsonInput.setPromptText(LangController.getText("PasteJson"));

        VBox center = new VBox(30, copyButtons, jsonInput);

        submit = new Button(LangController.getText("SubmitButton"));
        submit.getStyleClass().add("wordListButton");
        submit.setOnAction(e -> submitEvent());

        VBox submitVBox = new VBox(submit);
        submitVBox.setAlignment(Pos.CENTER);
        submitVBox.setPadding(new Insets(20, 0, 20, 0));

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(center);
        bp.setBottom(submitVBox);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private void submitEvent() {
        if (words != null && words.size() > 1) {
            ImportQuestionsView importQuestionsView = new ImportQuestionsView(scene, words);
            importQuestionsView.display(this);
            words = null;
            return;
        }
        if (words != null && words.size() == 1) {
            ImportQuestionsView importQuestionsView = new ImportQuestionsView(scene, words.getFirst());
            importQuestionsView.display(this);
            words = null;
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Question[] questions;
        try {
            questions = objectMapper.readValue(ViewUtils.fixJson(jsonInput.getText()), Question[].class);
        } catch (JsonProcessingException ex) {
            logger.error("Error while parsing Json", ex);

            ViewUtils.showAlert(Alert.AlertType.ERROR,
                    LangController.getText("JsonParseError"),
                    LangController.getText("ErrorText"));
            return;
        }

        EntityManager em = HibernateUtil.getEntityManager();
        HibernateUtil.startTransaction();

        word = em.find(Word.class, word.getId());

        for (Question question : questions) {
            word.getQuestions().add(question);
            question.setWord(word);
            em.persist(question);
        }

        em.merge(word);

        Question[] finalQuestions = questions;
        DoubleProperty progress = new SimpleDoubleProperty(0.0);
        new Thread(() -> {
            try {
                double progressValue = 1.0 / (finalQuestions.length + 1);
                for (Question question : finalQuestions) {
                    String ttsString = question.getJapanese();

                    File file = TTS.getTts().synthesizeAudio(ttsString, "audio/questions/" + question.getId() + ".wav");
                    question.setTtsPath(file.getPath());
                    em.merge(question);
                    progress.set(progress.get() + progressValue);
                }

            } catch (Exception ex) {
                logger.error("TTS API not available", ex);

                ViewUtils.showAlert(Alert.AlertType.ERROR,
                        LangController.getText("TTSNotAvailableError"),
                        LangController.getText("ErrorText"));
            } finally {
                HibernateUtil.commitTransaction();
                progress.setValue(1.0);
            }
        }).start();

        progress.addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0) {
                Platform.runLater(() -> this.origin.get().popToView());
            }
        });

        LoadingView loadingView = new LoadingView(scene);
        loadingView.bindProgress(progress);
        loadingView.display(this);
    }

    @Override
    public void popToView() {
        super.popToView();
        submitEvent();
    }

}
