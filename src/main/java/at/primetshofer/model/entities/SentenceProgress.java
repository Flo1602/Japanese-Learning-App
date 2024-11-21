package at.primetshofer.model.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class SentenceProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentenceProgress_id")
    private Sentence sentence;
    private LocalDateTime learned;
    private boolean correct;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
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
