package at.primetshofer.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class KanjiProgressDTO implements Serializable {

    private int id;
    private LocalDateTime learned;
    private int points;
    private int compressedEntries;

    public KanjiProgressDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getCompressedEntries() {
        return compressedEntries;
    }

    public void setCompressedEntries(int compressedEntries) {
        this.compressedEntries = compressedEntries;
    }
}
