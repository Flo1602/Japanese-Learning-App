package at.primetshofer.model.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WordDTO implements Serializable {

    private int id;
    private String japanese;
    private String english;
    private String kana;
    private String ttsPath;
    private boolean active = false;
    private LocalDate learned;
    private List<KanjiDTO> kanjis = new ArrayList<>();

    public WordDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJapanese() {
        return japanese;
    }

    public void setJapanese(String japanese) {
        this.japanese = japanese;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getKana() {
        return kana;
    }

    public void setKana(String kana) {
        this.kana = kana;
    }

    public String getTtsPath() {
        return ttsPath;
    }

    public void setTtsPath(String ttsPath) {
        this.ttsPath = ttsPath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getLearned() {
        return learned;
    }

    public void setLearned(LocalDate learned) {
        this.learned = learned;
    }

    public List<KanjiDTO> getKanjis() {
        return kanjis;
    }

    public void setKanjis(List<KanjiDTO> kanjis) {
        this.kanjis = kanjis;
    }
}
