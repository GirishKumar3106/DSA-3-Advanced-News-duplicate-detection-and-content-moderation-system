package com.advancednews.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.advancednews.model.DsaArticleRequest;
import com.advancednews.model.ManagedNewsArticle;

@Service
public class DsaNewsService {

    private final ArrayList<ManagedNewsArticle> articles =
            new ArrayList<>();

    private final TextMatchingService textMatchingService;

    private final Path dataFile =
            Paths.get("web-news.csv");

    private final String[] prohibitedWords = {
        "spam",
        "fakeoffer",
        "bannedword"
    };

    private final HashMap<String, String> keywordCategory =
            new HashMap<>();

    public DsaNewsService(
            TextMatchingService textMatchingService) {

        this.textMatchingService = textMatchingService;
        initializeCategories();
        loadFromFile();
    }

    private void initializeCategories() {

        keywordCategory.put("cricket", "Sports");
        keywordCategory.put("football", "Sports");
        keywordCategory.put("tennis", "Sports");
        keywordCategory.put("sports", "Sports");
        keywordCategory.put("match", "Sports");

        keywordCategory.put("election", "Politics");
        keywordCategory.put("government", "Politics");
        keywordCategory.put("minister", "Politics");
        keywordCategory.put("policy", "Politics");

        keywordCategory.put("technology", "Technology");
        keywordCategory.put("software", "Technology");
        keywordCategory.put("ai", "Technology");
        keywordCategory.put("artificial", "Technology");

        keywordCategory.put("movie", "Entertainment");
        keywordCategory.put("actor", "Entertainment");
        keywordCategory.put("film", "Entertainment");

        keywordCategory.put("business", "Business");
        keywordCategory.put("market", "Business");
    }

    // =========================================================
    // NEWS MANAGEMENT - ARRAYLIST + FILE HANDLING
    // =========================================================

    public synchronized Map<String, Object> addArticle(
            DsaArticleRequest request) {

        validate(request);

        boolean duplicateDetected =
                !checkDuplicate(
                        request.getTitle(),
                        request.getContent())
                 .get("matches").toString().equals("[]");

        String category =
                categorizeText(
                        request.getTitle(),
                        request.getContent());

        String moderationStatus =
                isModerated(request.getTitle(), request.getContent())
                ? "FLAGGED"
                : "APPROVED";

        int nextId = nextId();

        ManagedNewsArticle article =
                new ManagedNewsArticle(
                        nextId,
                        request.getTitle().trim(),
                        request.getContent().trim(),
                        category,
                        moderationStatus);

        articles.add(article);
        saveToFile();

        Map<String, Object> result =
                new HashMap<>();

        result.put("article", article);
        result.put("duplicateDetected", duplicateDetected);
        result.put("message", "Article added successfully");

        return result;
    }

    public synchronized List<ManagedNewsArticle> getAllArticles() {
        return new ArrayList<>(articles);
    }

    // =========================================================
    // DUPLICATE DETECTION - RABIN-KARP + EDIT DISTANCE / DP
    // =========================================================

    public synchronized Map<String, Object> checkDuplicate(
            String newTitle,
            String newContent) {

        validateText(newTitle, "Title");
        validateText(newContent, "Content");

        ArrayList<Map<String, Object>> matches =
                new ArrayList<>();

        for (ManagedNewsArticle existing : articles) {

            boolean exactTitle =
                    existing.getTitle()
                            .equalsIgnoreCase(newTitle.trim());

            boolean rabinKarpMatch =
                    textMatchingService.rabinKarpSearch(
                            existing.getTitle(),
                            newTitle);

            double titleSimilarity =
                    similarity(
                            existing.getTitle(),
                            newTitle);

            double contentSimilarity =
                    similarity(
                            existing.getContent(),
                            newContent);

            double overallSimilarity =
                    (titleSimilarity * 0.60)
                    + (contentSimilarity * 0.40);

            int commonKeywords =
                    countCommonKeywords(
                            newTitle,
                            existing.getTitle());

            boolean duplicate =
                    exactTitle
                    || rabinKarpMatch
                    || titleSimilarity >= 60
                    || overallSimilarity >= 55
                    || commonKeywords >= 2;

            if (duplicate) {

                Map<String, Object> match =
                        new HashMap<>();

                match.put("existingId", existing.getId());
                match.put("existingTitle", existing.getTitle());
                match.put("titleSimilarity",
                        round(titleSimilarity));
                match.put("contentSimilarity",
                        round(contentSimilarity));
                match.put("overallSimilarity",
                        round(overallSimilarity));
                match.put("commonKeywords", commonKeywords);
                match.put("rabinKarpMatch", rabinKarpMatch);

                matches.add(match);
            }
        }

        Map<String, Object> result =
                new HashMap<>();

        result.put("duplicateDetected", !matches.isEmpty());
        result.put("matches", matches);

        return result;
    }

    private int countCommonKeywords(
            String first,
            String second) {

        String[] words =
                first.toLowerCase()
                     .split("[^a-zA-Z]+");

        HashSet<String> checkedWords =
                new HashSet<>();

        int count = 0;

        for (String word : words) {

            if (word.length() < 4
                    || checkedWords.contains(word)) {
                continue;
            }

            checkedWords.add(word);

            if (textMatchingService.rabinKarpSearch(
                    second.toLowerCase(),
                    word)) {

                count++;
            }
        }

        return count;
    }

    private double similarity(
            String first,
            String second) {

        int distance =
                editDistance(first, second);

        int maxLength =
                Math.max(first.length(), second.length());

        if (maxLength == 0) {
            return 100.0;
        }

        return (1.0
                - ((double) distance / maxLength))
                * 100.0;
    }

    private int editDistance(
            String first,
            String second) {

        first = first.toLowerCase();
        second = second.toLowerCase();

        int n = first.length();
        int m = second.length();

        int[][] dp =
                new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= n; i++) {

            for (int j = 1; j <= m; j++) {

                if (first.charAt(i - 1)
                        == second.charAt(j - 1)) {

                    dp[i][j] =
                            dp[i - 1][j - 1];

                } else {

                    dp[i][j] =
                            1 + Math.min(
                                    dp[i][j - 1],
                                    Math.min(
                                            dp[i - 1][j],
                                            dp[i - 1][j - 1]
                                    )
                            );
                }
            }
        }

        return dp[n][m];
    }

    // =========================================================
    // CONTENT MODERATION - KMP
    // =========================================================

    public synchronized Map<String, Object> moderateArticle(
            int id) {

        ManagedNewsArticle article =
                findArticle(id);

        if (article == null) {
            throw new IllegalArgumentException(
                    "Article not found.");
        }

        String text =
                article.getTitle()
                + " "
                + article.getContent();

        String detectedWord = null;

        for (String word : prohibitedWords) {

            if (kmpSearch(text, word)) {
                detectedWord = word;
                break;
            }
        }

        if (detectedWord != null) {
            article.setModerationStatus("FLAGGED");
        } else {
            article.setModerationStatus("APPROVED");
        }

        saveToFile();

        Map<String, Object> result =
                new HashMap<>();

        result.put("article", article);
        result.put("status", article.getModerationStatus());
        result.put("patternDetected", detectedWord);

        return result;
    }

    private boolean isModerated(
            String title,
            String content) {

        String text = title + " " + content;

        for (String word : prohibitedWords) {
            if (kmpSearch(text, word)) {
                return true;
            }
        }

        return false;
    }

    private boolean kmpSearch(
            String text,
            String pattern) {

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        if (pattern.isEmpty()) {
            return true;
        }

        int[] lps = buildLPS(pattern);

        int i = 0;
        int j = 0;

        while (i < text.length()) {

            if (text.charAt(i) == pattern.charAt(j)) {

                i++;
                j++;

                if (j == pattern.length()) {
                    return true;
                }

            } else if (j != 0) {

                j = lps[j - 1];

            } else {
                i++;
            }
        }

        return false;
    }

    private int[] buildLPS(String pattern) {

        int[] lps =
                new int[pattern.length()];

        int length = 0;
        int i = 1;

        while (i < pattern.length()) {

            if (pattern.charAt(i)
                    == pattern.charAt(length)) {

                length++;
                lps[i] = length;
                i++;

            } else if (length != 0) {

                length = lps[length - 1];

            } else {

                lps[i] = 0;
                i++;
            }
        }

        return lps;
    }

    // =========================================================
    // CATEGORIZATION - HASHMAP
    // =========================================================

    public synchronized Map<String, Object> categorizeArticle(
            int id) {

        ManagedNewsArticle article =
                findArticle(id);

        if (article == null) {
            throw new IllegalArgumentException(
                    "Article not found.");
        }

        String category =
                categorizeText(
                        article.getTitle(),
                        article.getContent());

        article.setCategory(category);
        saveToFile();

        Map<String, Object> result =
                new HashMap<>();

        result.put("article", article);
        result.put("category", category);

        return result;
    }

    private String categorizeText(
            String title,
            String content) {

        String text =
                (title + " " + content)
                .toLowerCase();

        HashMap<String, Integer> categoryCount =
                new HashMap<>();

        for (Map.Entry<String, String> entry :
                keywordCategory.entrySet()) {

            String keyword = entry.getKey();
            String category = entry.getValue();

            if (text.contains(keyword)) {

                categoryCount.put(
                        category,
                        categoryCount.getOrDefault(
                                category,
                                0) + 1);
            }
        }

        String bestCategory = "General";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry :
                categoryCount.entrySet()) {

            if (entry.getValue() > highestCount) {

                highestCount = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    // =========================================================
    // TRENDING KEYWORDS - HASHMAP + PRIORITYQUEUE
    // =========================================================

    public synchronized List<Map<String, Object>> trendingKeywords() {

        HashMap<String, Integer> frequency =
                new HashMap<>();

        for (ManagedNewsArticle article : articles) {

            String text =
                    (article.getTitle() + " "
                    + article.getContent())
                    .toLowerCase();

            String[] words =
                    text.split("[^a-zA-Z]+");

            for (String word : words) {

                if (word.length() >= 4) {

                    frequency.put(
                            word,
                            frequency.getOrDefault(
                                    word,
                                    0) + 1);
                }
            }
        }

        PriorityQueue<Map.Entry<String, Integer>> pq =
                new PriorityQueue<>(
                        Comparator.comparing(
                                Map.Entry<String, Integer>::getValue)
                                .reversed()
                                .thenComparing(
                                        Map.Entry::getKey));

        pq.addAll(frequency.entrySet());

        ArrayList<Map<String, Object>> result =
                new ArrayList<>();

        int rank = 1;

        while (!pq.isEmpty() && rank <= 10) {

            Map.Entry<String, Integer> entry =
                    pq.poll();

            Map<String, Object> item =
                    new HashMap<>();

            item.put("rank", rank);
            item.put("keyword", entry.getKey());
            item.put("mentions", entry.getValue());

            result.add(item);
            rank++;
        }

        return result;
    }

    // =========================================================
    // RELATED NEWS - GRAPH + BFS
    // =========================================================

    public synchronized Map<String, Object> relatedNews(
            int startId) {

        ManagedNewsArticle start =
                findArticle(startId);

        if (start == null) {
            throw new IllegalArgumentException(
                    "Article not found.");
        }

        HashMap<Integer, ArrayList<Integer>> graph =
                new HashMap<>();

        for (ManagedNewsArticle article : articles) {
            graph.put(article.getId(), new ArrayList<>());
        }

        for (int i = 0; i < articles.size(); i++) {

            for (int j = i + 1; j < articles.size(); j++) {

                ManagedNewsArticle a = articles.get(i);
                ManagedNewsArticle b = articles.get(j);

                boolean related =
                        a.getCategory()
                         .equalsIgnoreCase(
                                 b.getCategory())
                        || haveCommonKeyword(a, b);

                if (related) {
                    graph.get(a.getId()).add(b.getId());
                    graph.get(b.getId()).add(a.getId());
                }
            }
        }

        Queue<Integer> queue =
                new ArrayDeque<>();

        HashSet<Integer> visited =
                new HashSet<>();

        ArrayList<ManagedNewsArticle> resultArticles =
                new ArrayList<>();

        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {

            int current = queue.poll();

            for (int neighbour :
                    graph.get(current)) {

                if (!visited.contains(neighbour)) {

                    visited.add(neighbour);
                    queue.add(neighbour);

                    ManagedNewsArticle article =
                            findArticle(neighbour);

                    if (article != null) {
                        resultArticles.add(article);
                    }
                }
            }
        }

        Map<String, Object> result =
                new HashMap<>();

        result.put("startArticle", start);
        result.put("relatedArticles", resultArticles);

        return result;
    }

    private boolean haveCommonKeyword(
            ManagedNewsArticle a,
            ManagedNewsArticle b) {

        String[] words =
                a.getTitle()
                 .toLowerCase()
                 .split("\\s+");

        String titleB =
                b.getTitle().toLowerCase();

        for (String word : words) {

            word = word.replaceAll("[^a-z]", "");

            if (word.length() >= 4
                    && textMatchingService.rabinKarpSearch(
                            titleB,
                            word)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // UTILITIES + FILE PERSISTENCE
    // =========================================================

    private ManagedNewsArticle findArticle(int id) {

        for (ManagedNewsArticle article : articles) {
            if (article.getId() == id) {
                return article;
            }
        }

        return null;
    }

    private int nextId() {

        int max = 0;

        for (ManagedNewsArticle article : articles) {
            max = Math.max(max, article.getId());
        }

        return max + 1;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void validate(
            DsaArticleRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required.");
        }

        validateText(request.getTitle(), "Title");
        validateText(request.getContent(), "Content");
    }

    private void validateText(
            String value,
            String field) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    field + " cannot be empty.");
        }
    }

    private void saveToFile() {

        try (BufferedWriter writer =
                Files.newBufferedWriter(
                        dataFile,
                        StandardCharsets.UTF_8)) {

            for (ManagedNewsArticle article : articles) {

                writer.write(
                        article.getId() + "|"
                        + clean(article.getTitle()) + "|"
                        + clean(article.getContent()) + "|"
                        + clean(article.getCategory()) + "|"
                        + clean(article.getModerationStatus()));

                writer.newLine();
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to save news data: "
                    + e.getMessage(),
                    e);
        }
    }

    private void loadFromFile() {

        if (!Files.exists(dataFile)) {
            return;
        }

        try (BufferedReader reader =
                Files.newBufferedReader(
                        dataFile,
                        StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data =
                        line.split("\\|", -1);

                if (data.length != 5) {
                    continue;
                }

                try {

                    articles.add(
                            new ManagedNewsArticle(
                                    Integer.parseInt(data[0]),
                                    data[1],
                                    data[2],
                                    data[3],
                                    data[4]));

                } catch (NumberFormatException ignored) {
                    // Skip malformed record.
                }
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to load news data: "
                    + e.getMessage(),
                    e);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("|", " ");
    }
}
