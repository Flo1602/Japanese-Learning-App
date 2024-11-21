package at.primetshofer.model.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @JsonProperty("japanese_text")
    private String japanese;
    @JsonProperty("english_question")
    private String question;
    private String ttsPath;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Word word;
    @ElementCollection
    @JsonProperty("answer_options")
    private Set<String> answers = new HashSet<>();
    @JsonProperty("correct_answer")
    private String correctAnswer;
    private int usedWordCount;

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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String english) {
        this.question = english;
    }

    public String getTtsPath() {
        return ttsPath;
    }

    public void setTtsPath(String ttsPath) {
        this.ttsPath = ttsPath;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public Set<String> getAnswers() {
        return answers;
    }

    public void setAnswers(Set<String> answers) {
        this.answers = answers;
    }

    public int getUsedWordCount() {
        return usedWordCount;
    }

    public void setUsedWordCount(int usedWordCount) {
        this.usedWordCount = usedWordCount;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
