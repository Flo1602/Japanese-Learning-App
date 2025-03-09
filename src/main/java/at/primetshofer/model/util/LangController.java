package at.primetshofer.model.util;

import at.primetshofer.logic.tracing.verification.VerificationLogic;
import org.apache.log4j.Logger;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LangController {

    private static final Logger logger = Logger.getLogger(LangController.class);

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
                logger.error("Failed to load language resource file", ex);
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