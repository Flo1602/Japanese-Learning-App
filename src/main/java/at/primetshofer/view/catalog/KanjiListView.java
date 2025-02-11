package at.primetshofer.view.catalog;

import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class KanjiListView extends View {

    private int scrollPage = 0;
    private ChangeListener scrollPageListener;
    private ScrollPane kanjiList;
    private TextField searchField;

    public KanjiListView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("KanjiListHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        Label searchLabel = new Label(LangController.getText("SearchLabel"));
        searchLabel.getStyleClass().add("normalText");

        searchField = new TextField();

        HBox searchBox = new HBox(searchLabel, searchField);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setSpacing(20);

        kanjiList = new ScrollPane();
        kanjiList.setFitToWidth(true);
        populateKanjiList();

        searchField.setOnAction(e -> {
            populateKanjiList();
        });

        VBox center = new VBox(searchBox, kanjiList);
        center.setAlignment(Pos.CENTER);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(center);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private void populateKanjiList() {
        scrollPage = 0;
        String search = searchField.getText();
        if (scrollPageListener != null) {
            kanjiList.vvalueProperty().removeListener(scrollPageListener);
        }
        VBox vb = new VBox();
        vb.setFillWidth(true);
        vb.setAlignment(Pos.CENTER);
        EntityManager em = HibernateUtil.getEntityManager();

        List<Kanji> kanjis = fetchKanji(em, 0, 50, search);
        List<KanjiCard> kanjiCards = new ArrayList<>();

        for (Kanji kanji : kanjis) {
            KanjiCard card = new KanjiCard(kanji);
            kanjiCards.add(card);
            vb.getChildren().add(card.getCard());
        }

        List<Kanji> nextNewKanji = fetchKanji(em, 5 * (scrollPage + 1), 50, search);
        List<Kanji> prevNewKanji = fetchKanji(em, 5 * (scrollPage), 50, search);
        AtomicBoolean listsUpdated = new AtomicBoolean(true);

        scrollPageListener = (ChangeListener<Number>) (observableValue, oldValue, newValue) -> {
            if (!listsUpdated.get() || kanjis.size() < 50) {
                return;
            }

            if (newValue.doubleValue() > 0.8) {
                scrollPage++;
                listsUpdated.set(false);
                int dif = kanjiCards.size() - nextNewKanji.size();
                for (int i = 0; i < nextNewKanji.size(); i++) {
                    KanjiCard card = kanjiCards.get(i + dif);

                    card.setSymbol(nextNewKanji.get(i).getSymbol());
                    card.setWords(kanjiWordListToString(nextNewKanji.get(i)));
                    card.setId(nextNewKanji.get(i).getId());
                }

                if (!nextNewKanji.isEmpty()) {
                    kanjiList.setVvalue(oldValue.doubleValue() - 0.1 - ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateKanjiLists(em, nextNewKanji, prevNewKanji, listsUpdated, search);
                } else {
                    scrollPage--;
                }
            }
            if (newValue.doubleValue() < 0.2 && scrollPage > 0) {
                scrollPage--;
                listsUpdated.set(false);
                for (int i = 0; i < prevNewKanji.size(); i++) {
                    KanjiCard card = kanjiCards.get(i);

                    card.setSymbol(prevNewKanji.get(i).getSymbol());
                    card.setWords(kanjiWordListToString(prevNewKanji.get(i)));
                    card.setId(prevNewKanji.get(i).getId());
                }

                if (!prevNewKanji.isEmpty()) {
                    kanjiList.setVvalue(oldValue.doubleValue() + 0.1 + ((Math.abs(oldValue.doubleValue() - newValue.doubleValue())) / 2));
                    updateKanjiLists(em, nextNewKanji, prevNewKanji, listsUpdated, search);
                } else {
                    scrollPage++;
                }
            }
        };

        kanjiList.setVvalue(0);
        kanjiList.setPrefHeight(500);
        kanjiList.setContent(vb);
        kanjiList.vvalueProperty().addListener(scrollPageListener);
    }

    private void updateKanjiLists(EntityManager em, List<Kanji> nextNewKanji, List<Kanji> prevNewKanji, AtomicBoolean listsUpdated, String search) {
        final int scrollPageFinal = scrollPage;
        new Thread(() -> {
            synchronized (nextNewKanji) {
                nextNewKanji.clear();
                nextNewKanji.addAll(fetchKanji(em, 5 * (scrollPageFinal + 1), 50, search));
                if (scrollPageFinal > 0) {
                    prevNewKanji.clear();
                    prevNewKanji.addAll(fetchKanji(em, 5 * (scrollPageFinal - 1), 50, search));
                }
                listsUpdated.set(true);
            }
        }).start();
    }

    private List<Kanji> fetchKanji(EntityManager em, int start, int limit, String searchString) {
        // Base JPQL query
        StringBuilder queryString = new StringBuilder("SELECT k FROM Kanji k");

        // Check if searchString is provided and add conditions to the query
        if (searchString != null && !searchString.isBlank()) {
            searchString = searchString.toLowerCase();
            queryString.append(" WHERE LOWER(k.symbol) LIKE '%" + searchString + "%'");
        }

        // Add sorting and create query
        queryString.append(" ORDER BY k.id DESC");
        TypedQuery<Kanji> query = em.createQuery(queryString.toString(), Kanji.class)
                .setFirstResult(start)
                .setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public void popToView() {
        super.popToView();
        populateKanjiList();
    }

    private String kanjiWordListToString(Kanji kanji) {
        List<String> wordsList = new ArrayList<>();

        for (Word word : kanji.getWords()) {
            wordsList.add(word.getJapanese());
        }

        return String.join(", ", wordsList);
    }

    private class KanjiCard {

        private final Label symbol;
        private final Label words;
        private int id;

        private KanjiCard(Kanji kanji) {
            this.id = kanji.getId();
            this.symbol = new Label(kanji.getSymbol());
            this.words = new Label(kanjiWordListToString(kanji));
        }

        private StackPane getCard() {
            StackPane card = new StackPane();
            card.setPadding(new Insets(5));
            card.setPrefWidth(900);
            card.setAlignment(Pos.CENTER);

            Pane cardBackground = new Pane();
            cardBackground.setPrefSize(300, 60);
            cardBackground.setStyle("-fx-background-color: #4DA8DA; -fx-background-radius: 25;");

            symbol.setStyle("-fx-font-size: 32pt; -fx-text-fill: white; -fx-font-weight: bold");
            words.setStyle("-fx-font-size: 16pt; -fx-text-fill: white");
            words.setMaxWidth(700);

            HBox labels = new HBox(20, symbol, words);
            labels.setAlignment(Pos.CENTER_LEFT);
            labels.setPadding(new Insets(0, 0, 0, 50));

            Button wordsButton = new Button(LangController.getText("WordsButton"));
            wordsButton.setStyle("-fx-background-radius: 20; -fx-font-size: 16pt");
            wordsButton.setOnAction(event -> {
                WordListView wordListView = new WordListView(scene, symbol.getText());
                wordListView.display(KanjiListView.this);
            });

            HBox controls = new HBox(wordsButton);
            controls.setPadding(new Insets(0, 50, 0, 0));
            controls.setAlignment(Pos.CENTER);

            BorderPane content = new BorderPane();
            content.setLeft(labels);
            content.setRight(controls);
            card.getChildren().addAll(cardBackground, content);

            return card;
        }

        public void setSymbol(String symbol) {
            this.symbol.setText(symbol);
        }

        public void setWords(String words) {
            this.words.setText(words);
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
