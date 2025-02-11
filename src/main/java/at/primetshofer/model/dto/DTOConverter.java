package at.primetshofer.model.dto;

import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.KanjiProgress;
import at.primetshofer.model.entities.Word;

import java.util.ArrayList;
import java.util.List;

public class DTOConverter {

    public static KanjiDTO kanjiToDTO(Kanji kanji) {
        KanjiDTO kanjiDTO = new KanjiDTO();
        kanjiDTO.setId(kanji.getId());
        kanjiDTO.setProgresses(kanjisProgressesToDTO(kanji.getProgresses()));
        kanjiDTO.setSymbol(kanji.getSymbol());
        kanjiDTO.setWords(wordsToDTO(kanji.getWords(), false));
        return kanjiDTO;
    }

    public static WordDTO wordToDTO(Word word, boolean includeKanji) {
        WordDTO wordDTO = new WordDTO();
        wordDTO.setId(word.getId());
        wordDTO.setEnglish(word.getEnglish());
        wordDTO.setActive(word.isActive());
        wordDTO.setJapanese(word.getJapanese());
        wordDTO.setKana(word.getKana());
        wordDTO.setTtsPath(word.getTtsPath());
        wordDTO.setLearned(word.getLearned());
        if (includeKanji) {
            ArrayList<KanjiDTO> kanjiDTOs = new ArrayList<>();
            for (Kanji kanji : word.getKanjis()) {
                kanjiDTOs.add(kanjiToDTO(kanji));
            }
            wordDTO.setKanjis(kanjiDTOs);
        }

        return wordDTO;
    }

    public static KanjiProgressDTO kanjiToProgressDTO(KanjiProgress kanjiProgress) {
        KanjiProgressDTO kanjiProgressDTO = new KanjiProgressDTO();
        kanjiProgressDTO.setId(kanjiProgress.getId());
        kanjiProgressDTO.setPoints(kanjiProgress.getPoints());
        kanjiProgressDTO.setCompressedEntries(kanjiProgress.getCompressedEntries());
        kanjiProgressDTO.setLearned(kanjiProgress.getLearned());
        return kanjiProgressDTO;
    }

    public static List<WordDTO> wordsToDTO(List<Word> words, boolean includeKanji) {
        ArrayList<WordDTO> wordDTOs = new ArrayList<>();
        for (Word word : words) {
            wordDTOs.add(wordToDTO(word, includeKanji));
        }
        return wordDTOs;
    }

    public static List<KanjiProgressDTO> kanjisProgressesToDTO(List<KanjiProgress> progresses) {
        ArrayList<KanjiProgressDTO> kanjiProgressDTOs = new ArrayList<>();
        for (KanjiProgress progress : progresses) {
            kanjiProgressDTOs.add(kanjiToProgressDTO(progress));
        }

        return kanjiProgressDTOs;
    }

    public static List<KanjiDTO> kanjisToDTO(List<Kanji> kanjis) {
        ArrayList<KanjiDTO> kanjisDTOs = new ArrayList<>();
        for (Kanji kanji : kanjis) {
            kanjisDTOs.add(kanjiToDTO(kanji));
        }

        return kanjisDTOs;
    }
}
