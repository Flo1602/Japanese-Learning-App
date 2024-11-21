package at.primetshofer.model.util;

import java.text.Normalizer;

public class StringSimilarity {

    public static double calculateSimilarity(String str1, String str2) {
        // Preprocess the strings
        String processedStr1 = preprocessString(str1);
        String processedStr2 = preprocessString(str2);

        // Calculate Levenshtein distance
        int distance = levenshteinDistance(processedStr1, processedStr2);

        // Calculate the maximum possible length
        int maxLength = Math.max(processedStr1.length(), processedStr2.length());

        // Avoid division by zero
        if (maxLength == 0) {
            return 100.0; // If both strings are empty, they are 100% similar
        }

        // Compute similarity percentage
        double similarity = ((maxLength - distance) / (double) maxLength) * 100.0;
        return similarity;
    }

    private static String preprocessString(String str) {
        // Normalize to remove diacritics and make consistent
        str = Normalizer.normalize(str, Normalizer.Form.NFKC);

        // Remove punctuation and spaces
        return str.replaceAll("\\p{Punct}|\\s", "");
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
