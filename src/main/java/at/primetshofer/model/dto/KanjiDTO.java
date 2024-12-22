package at.primetshofer.model.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class KanjiDTO implements Serializable {

    private int id;
    private String symbol;
    private List<WordDTO> words = new ArrayList<>();
    private List<KanjiProgressDTO> progresses = new ArrayList<>();

    public KanjiDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public List<WordDTO> getWords() {
        return words;
    }

    public void setWords(List<WordDTO> words) {
        this.words = words;
    }

    public List<KanjiProgressDTO> getProgresses() {
        return progresses;
    }

    public void setProgresses(List<KanjiProgressDTO> progresses) {
        this.progresses = progresses;
    }
}
