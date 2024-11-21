package at.primetshofer.model.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Sentence {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @JsonProperty("japanese_sentence")
    private String japanese;
    @JsonProperty("english_sentence")
    private String english;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id")
    private Word word;
    private String ttsPath;
    private int usedWordCount;
    @OneToMany(mappedBy = "sentence", cascade = CascadeType.ALL)
    private List<SentenceProgress> progresses = new ArrayList<>();
    @JsonProperty("word_list")
    @OneToMany(mappedBy = "sentence", cascade = CascadeType.ALL)
    private List<SentenceWord> sentenceWords = new ArrayList<>();
    @ElementCollection
    private Set<String> synonyms = new HashSet<>();

    public List<SentenceProgress> getProgresses() {
        return progresses;
    }

    public void setProgresses(List<SentenceProgress> progresses) {
        this.progresses = progresses;
    }

    public List<SentenceWord> getSentenceWords() {
        return sentenceWords;
    }

    public void setSentenceWords(List<SentenceWord> sentenceWords) {
        this.sentenceWords = sentenceWords;
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

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public String getTtsPath() {
        return ttsPath;
    }

    public void setTtsPath(String ttsPath) {
        this.ttsPath = ttsPath;
    }

    public int getUsedWordCount() {
        return usedWordCount;
    }

    public void setUsedWordCount(int usedWordCount) {
        this.usedWordCount = usedWordCount;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(Set<String> synonyms) {
        this.synonyms = synonyms;
    }
}
