package at.primetshofer.model.entities;

import at.primetshofer.model.util.Stylesheet;
import jakarta.persistence.*;

@Entity
public class Settings {

    @Id
    private int id;
    private int voiceId;
    private int newWords;
    private int maxDailyKanji;
    private Stylesheet styleSheet;

    public Stylesheet getStyleSheet() {
        return styleSheet;
    }

    public void setStyleSheet(Stylesheet styleSheetId) {
        this.styleSheet = styleSheetId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(int voide) {
        this.voiceId = voide;
    }

    public int getNewWords() {
        return newWords;
    }

    public void setNewWords(int newWords) {
        this.newWords = newWords;
    }

    public int getMaxDailyKanji() {
        return maxDailyKanji;
    }

    public void setMaxDailyKanji(int newKanji) {
        this.maxDailyKanji = newKanji;
    }
}
