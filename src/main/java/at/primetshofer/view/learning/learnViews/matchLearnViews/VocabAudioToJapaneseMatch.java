package at.primetshofer.view.learning.learnViews.matchLearnViews;

import at.primetshofer.model.entities.Word;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VocabAudioToJapaneseMatch extends MatchLearnView{

    private List<Word> words;

    public VocabAudioToJapaneseMatch(LearnSessionManager learnSessionManager, List<Word> words) {
        super(learnSessionManager);
        this.words = words;
    }

    @Override
    public Pane initView() {
        Map<String, String> wordPairs = new HashMap<>();
        Map<String, String> ttsPaths = new HashMap<>();
        for (Word word : words) {
            String japanese = ViewUtils.removeParenthesesContent(word.getJapanese()).split(",")[0];
            String id = word.getId() + "";
            wordPairs.put(id, japanese);
            ttsPaths.put(id, word.getTtsPath());
        }
        super.setMatchPairs(wordPairs);
        super.setTtsPaths(ttsPaths, true);

        return super.initView();
    }
}
