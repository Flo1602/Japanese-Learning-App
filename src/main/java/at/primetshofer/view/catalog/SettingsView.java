package at.primetshofer.view.catalog;

import at.primetshofer.model.Controller;
import at.primetshofer.model.TTS;
import at.primetshofer.model.entities.Settings;
import at.primetshofer.model.util.LangController;
import at.primetshofer.model.util.Stylesheet;
import at.primetshofer.view.ViewUtils;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

public class SettingsView extends View {


    private TextField voiceIdInput;
    private TextField newWordsInput;
    private TextField maxDailyKanjiInput;
    private TextField maxDailyWordsInput;
    private ComboBox<Stylesheet> styleSheetComboBox;

    public SettingsView(Scene scene) {
        super(scene);
    }

    protected void initView(){
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
        test.setOnAction(event ->{
            new Thread(() ->{
                try {
                    int id = TTS.getSpeakerId();
                    TTS.setSpeakerId(Integer.parseInt(voiceIdInput.getText()));
                    File file = TTS.getTts().synthesizeAudio("これはテストです", "audio/system/test.wav");
                    TTS.setSpeakerId(id);
                    Media media = new Media(file.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("TTS error!");
                        alert.showAndWait();
                    });
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

        vb.getChildren().addAll(voiceIdSetting, maxDailyKanjiSetting, maxDailyWordsSetting, styleSheetSetting);
        vb.setAlignment(Pos.CENTER);
        vb.getStyleClass().add("menuVBox");

        Button save = new Button(LangController.getText("saveButton"));
        save.getStyleClass().add("menuButton");
        save.setOnAction(e -> {
            Controller controller = Controller.getInstance();
            Settings settings = controller.getSettings();

            try{
                settings.setVoiceId(Integer.parseInt(voiceIdInput.getText()));
                settings.setNewWords(Integer.parseInt(newWordsInput.getText()));
                settings.setStyleSheet(styleSheetComboBox.getValue());
                settings.setMaxDailyKanji(Integer.parseInt(maxDailyKanjiInput.getText()));
                settings.setMaxDailyWords(Integer.parseInt(maxDailyWordsInput.getText()));

                controller.saveSettings();

                ViewUtils.applyStyleSheet();
                TTS.updateSpeakerId();

                origin.get().popToView();
            } catch (NumberFormatException ex){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Please enter a valid number!");
                alert.showAndWait();
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

    @Override
    public void display(View origin) {
        Settings settings = Controller.getInstance().getSettings();

        if(voiceIdInput != null){
            voiceIdInput.setText(settings.getVoiceId() + "");
        }
        if(newWordsInput != null){
            newWordsInput.setText(settings.getNewWords() + "");
        }
        if(styleSheetComboBox != null){
            styleSheetComboBox.setValue(settings.getStyleSheet());
        }
        if(maxDailyKanjiInput != null){
            maxDailyKanjiInput.setText(settings.getMaxDailyKanji() + "");
        }
        if(maxDailyWordsInput != null){
            maxDailyWordsInput.setText(settings.getMaxDailyWords() + "");
        }

        super.display(origin);
    }
}
