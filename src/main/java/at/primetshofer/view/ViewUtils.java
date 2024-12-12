package at.primetshofer.view;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.Controller;
import at.primetshofer.model.util.Stylesheet;
import at.primetshofer.view.catalog.View;
import atlantafx.base.theme.*;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ViewUtils {

    public static void applyStyleSheet(){
        Theme theme;

        switch (Controller.getInstance().getSettings().getStyleSheet()){
            case Stylesheet.CUPERTINO_LIGHT -> theme = new CupertinoLight();
            case Stylesheet.DRACULA -> theme = new Dracula();
            case Stylesheet.NORD_DARK -> theme = new NordDark();
            case Stylesheet.NORD_LIGHT -> theme = new NordLight();
            case Stylesheet.PRIMER_DARK -> theme = new PrimerDark();
            case Stylesheet.PRIMER_LIGHT -> theme = new PrimerLight();
            default -> theme = new CupertinoDark();
        }

        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
    }

    public static HBox getBackButtonBox(ObjectProperty<View> origin){
        Image backImage = new Image("back.png");

        ImageView backImageView = new ImageView(backImage);
        backImageView.setFitHeight(50);
        backImageView.setFitWidth(50);

        Button back = new Button();
        back.getStyleClass().add("settingsButton");
        back.setGraphic(backImageView);
        back.setOnAction(e -> {
            Controller.getInstance().stopAudio();
            AudioRecorder.stopRecording(null);
            origin.get().popToView();
        });

        HBox hb = new HBox(back);
        hb.getStyleClass().add("container2");
        hb.setAlignment(Pos.TOP_LEFT);

        return hb;
    }

    public static String removeParenthesesContent(String str) {
        return str.replaceAll("\\(.*?\\)", "").replaceAll("\\s{2,}", " ").trim();
    }

    public static ArrayList<String> splitByDelimiters(String input, Collection<String> delimiters) {
        ArrayList<String> result = new ArrayList<>();

        // Escape delimiters for regex and combine them into a single regex
        String regex = delimiters.stream()
                .map(d -> "\\Q" + d + "\\E") // Escape special characters in the delimiter
                .reduce((d1, d2) -> d1 + "|" + d2) // Combine with OR operator
                .orElse(""); // Handle empty list of delimiters

        if (regex.isEmpty()) {
            // If no delimiters provided, return the whole input as a single element
            result.add(input);
            return result;
        }

        // Use regex to split while keeping delimiters
        String[] parts = input.split("(?=" + regex + ")|(?<=" + regex + ")");
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }

        return result;
    }

    public static String cleanText(String input) {
        if (input == null || input.isEmpty()) {
            return input; // Return as is if input is null or empty
        }

        // Use regex to remove text matching the conditions
        return input.replaceAll("\\(.*?\\)", "")  // Remove text within parentheses
                .replaceAll("[,./:;!?]+.*", "") // Remove text after punctuation
                .replaceAll("[,./:;!?]", "")   // Remove standalone punctuation
                .trim(); // Trim leading/trailing spaces
    }

    public static String fixJson(String json) throws IllegalArgumentException {
        try {
            // Step 1: Try parsing the JSON directly
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json); // Validate JSON
            return json; // If valid, return as-is
        } catch (JsonProcessingException e) {
            // Step 2: If the JSON array is malformed, try to repair it
            List<String> validObjects = extractValidObjects(json);
            if (validObjects.isEmpty()) {
                throw new IllegalArgumentException("No valid objects found in JSON.");
            }

            // Step 3: Reassemble the JSON array with valid objects
            StringBuilder fixedJson = new StringBuilder("[");
            for (int i = 0; i < validObjects.size(); i++) {
                fixedJson.append(validObjects.get(i));
                if (i < validObjects.size() - 1) {
                    fixedJson.append(",");
                }
            }
            fixedJson.append("]");

            return fixedJson.toString();
        }
    }

    private static List<String> extractValidObjects(String json) {
        List<String> validObjects = new ArrayList<>();
        StringBuilder currentObject = new StringBuilder();
        boolean insideObject = false;
        int openBraces = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '{') {
                insideObject = true;
                openBraces++;
            }

            if (insideObject) {
                currentObject.append(c);
            }

            if (c == '}') {
                openBraces--;
                if (openBraces == 0) {
                    insideObject = false;

                    // Try parsing the current object
                    String objectString = currentObject.toString();
                    if (isValidJsonObject(objectString)) {
                        validObjects.add(objectString);
                    }
                    currentObject.setLength(0); // Clear the StringBuilder for the next object
                }
            }
        }

        return validObjects;
    }

    private static boolean isValidJsonObject(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(json); // Parse the JSON
            return true; // If parsing succeeds, it's valid
        } catch (JsonProcessingException e) {
            return false; // If parsing fails, it's invalid
        }
    }

    public static String convertKanjiToKatakana(String text) {
        Tokenizer tokenizer = new Tokenizer();
        StringBuilder hiraganaResult = new StringBuilder();

        for (Token token : tokenizer.tokenize(text)) {
            String reading = token.getReading();
            if (reading != null) {
                // Convert Katakana reading to Hiragana
                StringBuilder katakana = new StringBuilder();
                for (char c : reading.toCharArray()) {
                    katakana.append(c);
                }
                hiraganaResult.append(katakana);
            } else {
                // If no reading is available, use the surface form
                hiraganaResult.append(token.getSurface());
            }
        }

        return hiraganaResult.toString();
    }

    public static void showAlert(Alert.AlertType alertType, String message, String title) {
        Platform.runLater(() ->{
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
