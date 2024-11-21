package at.primetshofer.model.util;

import java.util.ArrayList;
import java.util.List;

public class JapaneseUtil {

    public static List<String> extractKanji(String text) {
        List<String> kanjiList = new ArrayList<>();

        // Regular expression for Kanji characters (Unicode range for Kanji)
        String kanjiRegex = "[\\p{InCJK_Unified_Ideographs}]";

        // Traverse each character in the input text
        for (char c : text.toCharArray()) {
            // If the character matches the Kanji regex, add it to the list
            if (String.valueOf(c).matches(kanjiRegex)) {
                kanjiList.add(String.valueOf(c));
            }
        }
        return kanjiList;
    }
}
