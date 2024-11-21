package at.primetshofer.view;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.Controller;
import at.primetshofer.model.util.Stylesheet;
import at.primetshofer.view.catalog.View;
import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Collection;

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
}
