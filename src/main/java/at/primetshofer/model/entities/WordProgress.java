package at.primetshofer.model.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WordProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wordProgress_id")
    private Word word;
    private LocalDateTime learned;
    private boolean correct;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public LocalDateTime getLearned() {
        return learned;
    }

    public void setLearned(LocalDateTime learned) {
        this.learned = learned;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
