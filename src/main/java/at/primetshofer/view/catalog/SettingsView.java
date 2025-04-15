package at.primetshofer.view.catalog;

import at.primetshofer.model.Controller;
import at.primetshofer.model.TTS;
import at.primetshofer.model.entities.Settings;
import at.primetshofer.model.util.LangController;
import at.primetshofer.model.util.Stylesheet;
import at.primetshofer.view.ViewUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.StringConverter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Locale;

public class SettingsView extends View {

    private static final Logger logger = Logger.getLogger(SettingsView.class);

    private TextField voiceIdInput;
    private TextField newWordsInput;
    private TextField maxDailyKanjiInput;
    private TextField maxDailyWordsInput;
    private ComboBox<Stylesheet> styleSheetComboBox;
    private ComboBox<Locale> languageComboBox;

    public SettingsView(Scene scene) {
        super(scene);
    }

    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("SettingsHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        VBox vb = new VBox();

        Label voidIdLabel = new Label(LangController.getText("voiceIdLabel"));
        voidIdLabel.getStyleClass().add("normalText");

        voiceIdInput = new TextField();

        voiceIdInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        Button test = new Button(LangController.getText("TestButton"));
        test.getStyleClass().add("normalButton");
        test.setOnAction(event -> {
            new Thread(() -> {
                try {
                    int id = TTS.getSpeakerId();
                    TTS.setSpeakerId(Integer.parseInt(voiceIdInput.getText()));
                    File file = TTS.getTts().synthesizeAudio("これはテストです", "audio/system/test.wav");
                    TTS.setSpeakerId(id);
                    Media media = new Media(file.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } catch (Exception ex) {
                    logger.error("TTS error", ex);

                    ViewUtils.showAlert(Alert.AlertType.ERROR,
                            LangController.getText("TTSNotAvailableError"),
                            LangController.getText("ErrorText"));
                }
            }).start();
        });

        HBox voiceIdSetting = new HBox(voidIdLabel, voiceIdInput, test);
        voiceIdSetting.getStyleClass().add("settingsHB");

        Label newWordsLabel = new Label(LangController.getText("newWordsLabel"));
        newWordsLabel.getStyleClass().add("normalText");

        newWordsInput = new TextField();

        newWordsInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        HBox newWordsSetting = new HBox(newWordsLabel, newWordsInput);
        newWordsSetting.getStyleClass().add("settingsHB");

        Label maxDailyKanjiLabel = new Label(LangController.getText("MaxDailyKanjiLabel"));
        maxDailyKanjiLabel.getStyleClass().add("normalText");

        maxDailyKanjiInput = new TextField();

        maxDailyKanjiInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        HBox maxDailyKanjiSetting = new HBox(maxDailyKanjiLabel, maxDailyKanjiInput);
        maxDailyKanjiSetting.getStyleClass().add("settingsHB");

        Label maxDailyWordsLabel = new Label(LangController.getText("MaxDailyWordsLabel"));
        maxDailyWordsLabel.getStyleClass().add("normalText");

        maxDailyWordsInput = new TextField();

        maxDailyWordsInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        HBox maxDailyWordsSetting = new HBox(maxDailyWordsLabel, maxDailyWordsInput);
        maxDailyWordsSetting.getStyleClass().add("settingsHB");

        Label styleSheetLabel = new Label(LangController.getText("styleSheetLabel"));
        styleSheetLabel.getStyleClass().add("normalText");

        styleSheetComboBox = new ComboBox<>();
        styleSheetComboBox.getItems().addAll(Stylesheet.values());

        HBox styleSheetSetting = new HBox(styleSheetLabel, styleSheetComboBox);
        styleSheetSetting.getStyleClass().add("settingsHB");

        Label languageLabel = new Label(LangController.getText("languageLabel"));
        languageLabel.getStyleClass().add("normalText");

        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll(LangController.supportedLocales);

        addCellFactoryForLanguageComboBox();

        HBox languageSetting = new HBox(languageLabel, languageComboBox);
        languageSetting.getStyleClass().add("settingsHB");

        vb.getChildren().addAll(voiceIdSetting, maxDailyKanjiSetting, maxDailyWordsSetting, styleSheetSetting, languageSetting);
        vb.setAlignment(Pos.CENTER);
        vb.getStyleClass().add("menuVBox");

        Button save = new Button(LangController.getText("saveButton"));
        save.getStyleClass().add("menuButton");
        save.setOnAction(e -> {
            Controller controller = Controller.getInstance();
            Settings settings = controller.getSettings();

            try {
                if(!settings.getLocale().equals(languageComboBox.getValue())){
                    ViewUtils.showAlert(Alert.AlertType.WARNING,
                            LangController.getText("RestartNeededForLanguageChange"),
                            LangController.getText("WarningText"));
                }

                settings.setVoiceId(Integer.parseInt(voiceIdInput.getText()));
                settings.setNewWords(Integer.parseInt(newWordsInput.getText()));
                settings.setStyleSheet(styleSheetComboBox.getValue());
                settings.setMaxDailyKanji(Integer.parseInt(maxDailyKanjiInput.getText()));
                settings.setMaxDailyWords(Integer.parseInt(maxDailyWordsInput.getText()));
                settings.setLocale(languageComboBox.getValue());

                controller.saveSettings();

                ViewUtils.applyStyleSheet();
                TTS.updateSpeakerId();

                origin.get().popToView();
            } catch (NumberFormatException ex) {
                logger.error("User input was invalid", ex);

                ViewUtils.showAlert(Alert.AlertType.ERROR,
                        LangController.getText("EnterValidNumberText"),
                        LangController.getText("ErrorText"));
            }
        });
        BorderPane.setAlignment(save, Pos.CENTER);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(vb);
        bp.setBottom(save);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private void addCellFactoryForLanguageComboBox() {
        languageComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                if (locale == null) return "";
                return locale.getDisplayLanguage(locale); // "Deutsch", "English", "日本語", etc.
            }

            @Override
            public Locale fromString(String string) {
                // Optional: implement if you want to support parsing back from String
                return null;
            }
        });

        languageComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Locale locale, boolean empty) {
                super.updateItem(locale, empty);
                setText(empty || locale == null ? null : locale.getDisplayLanguage(locale));
            }
        });
    }

    @Override
    public void display(View origin) {
        Settings settings = Controller.getInstance().getSettings();

        if (voiceIdInput != null) {
            voiceIdInput.setText(settings.getVoiceId() + "");
        }
        if (newWordsInput != null) {
            newWordsInput.setText(settings.getNewWords() + "");
        }
        if (styleSheetComboBox != null) {
            styleSheetComboBox.setValue(settings.getStyleSheet());
        }
        if (maxDailyKanjiInput != null) {
            maxDailyKanjiInput.setText(settings.getMaxDailyKanji() + "");
        }
        if (maxDailyWordsInput != null) {
            maxDailyWordsInput.setText(settings.getMaxDailyWords() + "");
        }
        if (languageComboBox != null) {
            languageComboBox.setValue(settings.getLocale());
        }

        super.display(origin);
    }
}
