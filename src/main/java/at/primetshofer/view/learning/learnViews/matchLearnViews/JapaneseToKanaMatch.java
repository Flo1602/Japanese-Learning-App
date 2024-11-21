package at.primetshofer.view.learning.learnViews.matchLearnViews;

import at.primetshofer.model.entities.Word;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JapaneseToKanaMatch extends MatchLearnView{

    private List<Word> words;
    private boolean reverse;

    public JapaneseToKanaMatch(LearnSessionManager learnSessionManager, List<Word> words) {
        super(learnSessionManager);
        this.words = words;
        this.reverse = false;
    }

    @Override
    public Pane initView() {
        Map<String, String> wordPairs = new HashMap<>();
        Map<String, String> ttsPaths = new HashMap<>();
        for (Word word : words) {
            String kana = ViewUtils.removeParenthesesContent(word.getKana()).split(",")[0];
            String japanese = ViewUtils.removeParenthesesContent(word.getJapanese()).split(",")[0];
            if(reverse){
                wordPairs.put(japanese, kana);
            } else {
                wordPairs.put(kana, japanese);
            }
            ttsPaths.put(word.getJapanese(), word.getTtsPath());
        }
        super.setMatchPairs(wordPairs);
        super.setTtsPaths(ttsPaths, false);

        return super.initView();
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }
}
