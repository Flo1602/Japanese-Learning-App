package at.primetshofer.model.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
public class SentenceWord {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentenceWord_id")
    private Sentence sentence;
    @Id
    private int wordPos;
    @JsonProperty("japanese_word")
    private String wordJapanese;
    @JsonProperty("english_translation")
    private String wordEnglish;
    @JsonProperty("kana_word")
    private String wordKana;

    public String getWordKana() {
        return wordKana;
    }

    public void setWordKana(String wordKana) {
        this.wordKana = wordKana;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
    }

    public int getWordPos() {
        return wordPos;
    }

    public void setWordPos(int wordPos) {
        this.wordPos = wordPos;
    }

    public String getWordJapanese() {
        return wordJapanese;
    }

    public void setWordJapanese(String wordJapanese) {
        this.wordJapanese = wordJapanese;
    }

    public String getWordEnglish() {
        return wordEnglish;
    }

    public void setWordEnglish(String wordEnglish) {
        this.wordEnglish = wordEnglish;
    }
}
