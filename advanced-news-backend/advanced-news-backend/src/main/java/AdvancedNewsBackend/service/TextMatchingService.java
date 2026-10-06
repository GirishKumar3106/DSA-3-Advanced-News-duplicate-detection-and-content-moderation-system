package com.advancednews.service;

import org.springframework.stereotype.Service;

@Service
public class TextMatchingService {

    // Rabin-Karp pattern searching
    public boolean rabinKarpSearch(
            String text,
            String pattern) {

        if (text == null || pattern == null) {
            return false;
        }

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int n = text.length();
        int m = pattern.length();

        if (m == 0) {
            return true;
        }

        if (m > n) {
            return false;
        }

        int base = 256;
        int prime = 101;

        int patternHash = 0;
        int textHash = 0;
        int highestPower = 1;

        for (int i = 0; i < m - 1; i++) {
            highestPower =
                    (highestPower * base) % prime;
        }

        for (int i = 0; i < m; i++) {

            patternHash =
                    (base * patternHash
                    + pattern.charAt(i)) % prime;

            textHash =
                    (base * textHash
                    + text.charAt(i)) % prime;
        }

        for (int i = 0; i <= n - m; i++) {

            if (patternHash == textHash) {

                boolean match = true;

                for (int j = 0; j < m; j++) {

                    if (text.charAt(i + j)
                            != pattern.charAt(j)) {

                        match = false;
                        break;
                    }
                }

                if (match) {
                    return true;
                }
            }

            if (i < n - m) {

                textHash =
                        (base * (
                            textHash
                            - text.charAt(i)
                            * highestPower)
                        + text.charAt(i + m))
                        % prime;

                if (textHash < 0) {
                    textHash += prime;
                }
            }
        }

        return false;
    }



    // Count meaningful keywords from claim
    // that appear in an article title.



    public double keywordCoverage(
        String claim,
        String comparedText) {

    if (claim == null || comparedText == null) {
        return 0;
    }

    String[] words =
            claim.toLowerCase()
                 .split("[^a-zA-Z]+");

    java.util.HashSet<String> uniqueWords =
            new java.util.HashSet<>();

    for (String word : words) {

        if (word.length() >= 4) {
            uniqueWords.add(word);
        }
    }

    if (uniqueWords.isEmpty()) {
        return 0;
    }

    int matched = 0;

    for (String word : uniqueWords) {

        if (rabinKarpSearch(
                comparedText,
                word)) {

            matched++;
        }
    }

    return ((double) matched
            / uniqueWords.size()) * 100;
}
    public int countMatchingKeywords(
            String claim,
            String title) {

        if (claim == null || title == null) {
            return 0;
        }

        String[] words =
                claim.toLowerCase()
                     .split("[^a-zA-Z]+");

        int count = 0;

        for (String word : words) {

            // Ignore very short/common words
            if (word.length() < 4) {
                continue;
            }

            if (rabinKarpSearch(title, word)) {
                count++;
            }
        }

        return count;
    }
}