package at.primetshofer.model.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class KanjiProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanjiProgress_id")
    private Kanji kanji;
    private LocalDateTime learned;
    private int points;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Kanji getKanji() {
        return kanji;
    }

    public void setKanji(Kanji kanji) {
        this.kanji = kanji;
    }

    public LocalDateTime getLearned() {
        return learned;
    }

    public void setLearned(LocalDateTime learned) {
        this.learned = learned;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
