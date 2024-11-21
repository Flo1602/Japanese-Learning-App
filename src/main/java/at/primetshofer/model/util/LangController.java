package at.primetshofer.model.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LangController {

    static final String BASENAME = "lang";

    private static ResourceBundle langController;

    static {
        LangController.initBundle(Locale.ENGLISH);
    }

    public static void initBundle(Locale lang) {
        try {
            langController = ResourceBundle.getBundle(BASENAME, lang);
            langController.getString("validFile");
        } catch (MissingResourceException e) {
            try {
                langController = ResourceBundle.getBundle(BASENAME, new Locale("en"));
                langController.getString("validFile");
            } catch (MissingResourceException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String getText(String id) {
        return langController.getString(id);
    }

    public static ResourceBundle getBundle() {
        return langController;
    }
}