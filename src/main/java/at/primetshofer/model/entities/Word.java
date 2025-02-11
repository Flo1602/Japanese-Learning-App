package at.primetshofer.model.entities;

import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.JapaneseUtil;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String japanese;
    private String english;
    private String kana;
    private String ttsPath;
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL)
    private List<WordProgress> progresses = new ArrayList<>();
    @ManyToMany(mappedBy = "words", cascade = CascadeType.PERSIST)
    private List<Kanji> kanjis = new ArrayList<>();
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL)
    private List<Sentence> sentences = new ArrayList<>();
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();
    private boolean active = false;
    private LocalDate learned;

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public String getKana() {
        return kana;
    }

    public void setKana(String kana) {
        this.kana = kana;
    }

    public List<WordProgress> getProgresses() {
        return progresses;
    }

    public void setProgresses(List<WordProgress> progresses) {
        this.progresses = progresses;
    }

    public List<Kanji> getKanjis() {
        return kanjis;
    }

    public void setKanjis(List<Kanji> kanjis) {
        this.kanjis = kanjis;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (!this.active && active) {
            learned = LocalDate.now();
        }
        if (this.active && !active) {
            learned = null;
        }

        this.active = active;
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

        connectKanji();
    }

    public void setJapaneseIgnoreKanji(String japanese) {
        this.japanese = japanese;
    }

    public void connectKanji() {
        List<String> kanjis = JapaneseUtil.extractKanji(japanese);
        EntityManager em = HibernateUtil.getEntityManager();

        for (String kanji : kanjis) {
            String jpql = "SELECT k FROM Kanji k WHERE k.symbol = '" + kanji + "'";
            TypedQuery<Kanji> query = HibernateUtil.getEntityManager().createQuery(jpql, Kanji.class);
            List<Kanji> foundKanji = query.getResultList();
            Kanji k;
            if (!foundKanji.isEmpty()) {
                k = foundKanji.getFirst();
            } else {
                k = new Kanji();
                k.setSymbol(kanji);
            }

            if (!k.getWords().contains(this)) {
                k.getWords().add(this);
                k = em.merge(k);
            }
            if (!this.kanjis.contains(k)) {
                this.kanjis.add(k);
            }
        }

        List<Kanji> removeKanjis = new ArrayList<>();
        for (Kanji kanji : this.kanjis) {
            if (!kanjis.contains(kanji.getSymbol())) {
                removeKanjis.add(kanji);
                kanji.getWords().remove(this);
                em.merge(kanji);
            }
        }

        this.kanjis.removeAll(removeKanjis);
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getTtsPath() {
        return ttsPath;
    }

    public void setTtsPath(String ttsPath) {
        this.ttsPath = ttsPath;
    }

    public LocalDate getLearned() {
        return learned;
    }

    public void setLearned(LocalDate learned) {
        this.learned = learned;
    }
}
