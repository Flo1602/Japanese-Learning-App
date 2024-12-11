package at.primetshofer.model.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "KANJI", uniqueConstraints = {
        @UniqueConstraint(columnNames = "symbol")
})
public class Kanji {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String symbol;
    @ManyToMany
    @JoinTable(
            name = "kanji_words",
            joinColumns = @JoinColumn(name = "kanji_id"),
            inverseJoinColumns = @JoinColumn(name = "word_id")
    )
    private List<Word> words = new ArrayList<Word>();
    @OneToMany(mappedBy = "kanji", cascade = CascadeType.ALL)
    private List<KanjiProgress> progresses = new ArrayList<>();

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

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public List<KanjiProgress> getProgresses() {
        return progresses;
    }

    public void setProgresses(List<KanjiProgress> progresses) {
        this.progresses = progresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kanji kanji = (Kanji) o;
        return Objects.equals(symbol, kanji.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(symbol);
    }
}
