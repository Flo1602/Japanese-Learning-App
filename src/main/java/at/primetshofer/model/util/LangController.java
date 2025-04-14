package at.primetshofer.model.util;

import org.apache.log4j.Logger;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LangController {

    private static final Logger logger = Logger.getLogger(LangController.class);

    static final String BASENAME = "lang";

    private static ResourceBundle langController;
    private static ResourceBundle fallback;

    public final static Locale[] supportedLocales = {
            Locale.ENGLISH,
            Locale.GERMAN,
            Locale.JAPANESE
    };

    static {
        LangController.initBundle(Locale.ENGLISH);
    }

    public static void initBundle(Locale lang) {
        try {
            fallback = ResourceBundle.getBundle(BASENAME, Locale.ENGLISH);
            langController = ResourceBundle.getBundle(BASENAME, lang);
            langController.getString("validFile");
        } catch (MissingResourceException e) {
            try {
                langController = ResourceBundle.getBundle(BASENAME, Locale.ENGLISH);
                langController.getString("validFile");
            } catch (MissingResourceException ex) {
                logger.error("Failed to load language resource file", ex);
            }
        }
    }

    public static String getText(String id) {
        try {
            return langController.getString(id);
        } catch (MissingResourceException e) {
            logger.warn("Failed to load text from resource file, trying again with fallback! Text ID: " + id);
            try {
                return fallback.getString(id);
            } catch (MissingResourceException ex) {
                logger.error("Failed to load language resource file", ex);
                return "-";
            }
        }
    }

    public static ResourceBundle getBundle() {
        return langController;
    }
}