package com.advancednews.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.advancednews.model.Article;
import com.advancednews.model.FactCheckEvidence;
import com.advancednews.model.NewsApiResponse;
import com.advancednews.model.VerificationResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class VerificationService {

    private final NewsApiService newsApiService;
    private final FactCheckService factCheckService;
    private final TextMatchingService textMatchingService;

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    public VerificationService(
            NewsApiService newsApiService,
            FactCheckService factCheckService,
            TextMatchingService textMatchingService) {

        this.newsApiService = newsApiService;
        this.factCheckService = factCheckService;
        this.textMatchingService = textMatchingService;
    }

    public VerificationResult verify(String claim)
            throws Exception {

        // =====================================================
        // 1. SEARCH ONLINE NEWS
        // =====================================================

        NewsApiResponse newsResponse =
                newsApiService.searchNews(claim);

        List<Article> articles =
                newsResponse.getArticles();

        int matchingArticles = 0;

        Set<String> sourceSet =
                new HashSet<>();

        if (articles != null) {

            for (Article article : articles) {

                if (article == null
                        || article.getTitle() == null) {
                    continue;
                }

                double similarity =
                        calculateSimilarity(
                                claim,
                                article.getTitle());

                int matchingKeywords =
                        textMatchingService.countMatchingKeywords(
                                claim,
                                article.getTitle());

                boolean rabinKarpMatch =
                        textMatchingService.rabinKarpSearch(
                                article.getTitle(),
                                claim);

                if (similarity >= 45
                        || matchingKeywords >= 2
                        || rabinKarpMatch) {

                    matchingArticles++;

                    if (article.getSource() != null
                            && article.getSource().getName() != null) {

                        sourceSet.add(
                                article.getSource().getName());
                    }
                }
            }
        }

        // =====================================================
        // 2. SEARCH GOOGLE FACT CHECK API
        // =====================================================

        String factCheckJson =
                factCheckService.searchFactChecks(claim);

        JsonNode root =
                jsonMapper.readTree(factCheckJson);

        JsonNode claims =
                root.get("claims");

        int factChecksReturned = 0;
        int relevantFactChecks = 0;
        int relatedFactChecks = 0;

        List<FactCheckEvidence> factChecks =
                new ArrayList<>();

        int falseCount = 0;
        int trueCount = 0;
        int mixedCount = 0;
        int misleadingCount = 0;

        if (claims != null && claims.isArray()) {

            factChecksReturned =
                    claims.size();

            for (JsonNode claimNode : claims) {

                String returnedClaim =
                        getText(
                                claimNode,
                                "text");

                if (returnedClaim.isEmpty()) {
                    continue;
                }

                // -------------------------------------------------
                // Compare user's claim with Google's returned claim
                // -------------------------------------------------

                double claimSimilarity =
                        calculateSimilarity(
                                claim,
                                returnedClaim);

                double keywordCoverage =
                        textMatchingService.keywordCoverage(
                                claim,
                                returnedClaim);

                double matchScore =
                        (claimSimilarity * 0.60)
                        + (keywordCoverage * 0.40);

                String matchType;

                if (matchScore >= 65) {

                    matchType = "STRONG";

                } else if (matchScore >= 40) {

                    matchType = "RELATED";

                } else {

                    continue;
                }

                // -------------------------------------------------
                // Read claim review information
                // -------------------------------------------------

                JsonNode reviews =
                        claimNode.get("claimReview");

                if (reviews == null
                        || !reviews.isArray()) {
                    continue;
                }

                for (JsonNode review : reviews) {

                    String publisher =
                            getNestedText(
                                    review,
                                    "publisher",
                                    "name");

                    String rating =
                            getText(
                                    review,
                                    "textualRating");

                    String title =
                            getText(
                                    review,
                                    "title");

                    String url =
                            getText(
                                    review,
                                    "url");

                    if (rating.isEmpty()) {
                        continue;
                    }

                    if ("STRONG".equals(matchType)) {

                        relevantFactChecks++;

                    } else {

                        relatedFactChecks++;
                    }

                    factChecks.add(
                            new FactCheckEvidence(
                                    returnedClaim,
                                    publisher,
                                    rating,
                                    title,
                                    url,
                                    matchScore,
                                    matchType
                            )
                    );

                    String ratingType =
                            classifyRating(rating);

                    switch (ratingType) {

                    case "FALSE":
                        falseCount++;
                        break;

                    case "TRUE":
                        trueCount++;
                        break;

                    case "MIXED":
                        mixedCount++;
                        break;

                    case "MISLEADING":
                        misleadingCount++;
                        break;

                    default:
                        break;
                    }
                }
            }
        }

        // =====================================================
        // 3. DETERMINE FINAL VERDICT
        // =====================================================

        String verdict =
                determineVerdict(
                        falseCount,
                        trueCount,
                        mixedCount,
                        misleadingCount,
                        relevantFactChecks,
                        relatedFactChecks,
                        matchingArticles,
                        sourceSet.size()
                );

        // =====================================================
        // 4. CALCULATE CONFIDENCE
        // =====================================================

        int confidence =
                calculateConfidence(
                        falseCount,
                        trueCount,
                        mixedCount,
                        misleadingCount,
                        relevantFactChecks,
                        relatedFactChecks,
                        matchingArticles,
                        sourceSet.size()
                );

        // =====================================================
        // 5. RETURN RESULT
        // =====================================================

        return new VerificationResult(
                claim,
                verdict,
                confidence,
                matchingArticles,
                sourceSet.size(),
                factChecksReturned,
                relevantFactChecks,
                relatedFactChecks,
                new ArrayList<>(sourceSet),
                factChecks
        );
    }

    // =========================================================
    // VERDICT LOGIC
    // =========================================================

    private String determineVerdict(
            int falseCount,
            int trueCount,
            int mixedCount,
            int misleadingCount,
            int relevantFactChecks,
            int relatedFactChecks,
            int matchingArticles,
            int uniqueSources) {

        // Strong fact-check evidence
        if (relevantFactChecks > 0) {

            if (falseCount > 0
                    && trueCount > 0) {

                return "CONFLICTING FACT CHECKS";
            }

            if (falseCount > 0) {
                return "FACT-CHECKED FALSE";
            }

            if (misleadingCount > 0) {
                return "FACT-CHECKED MISLEADING";
            }

            if (mixedCount > 0) {
                return "PARTIALLY TRUE / MIXED";
            }

            if (trueCount > 0) {
                return "FACT-CHECKED TRUE";
            }

            return "FACT-CHECK AVAILABLE";
        }

        // Related fact-check evidence
        if (relatedFactChecks > 0) {
            return "RELATED FACT-CHECK FOUND";
        }

        // Online source agreement
        if (uniqueSources >= 3
                && matchingArticles >= 3) {

            return "SUPPORTED BY MULTIPLE SOURCES";
        }

        if (uniqueSources >= 2
                && matchingArticles >= 2) {

            return "LIKELY SUPPORTED";
        }

        return "NEEDS VERIFICATION";
    }

    // =========================================================
    // CONFIDENCE SCORE
    // =========================================================

    private int calculateConfidence(
            int falseCount,
            int trueCount,
            int mixedCount,
            int misleadingCount,
            int relevantFactChecks,
            int relatedFactChecks,
            int matchingArticles,
            int uniqueSources) {

        // Strong fact-check result
        if (relevantFactChecks > 0) {

            if (falseCount > 0
                    && trueCount > 0) {

                return 55;
            }

            if (falseCount > 0
                    || misleadingCount > 0) {

                return Math.min(
                        90 + Math.min(
                                relevantFactChecks * 2,
                                8),
                        98);
            }

            if (trueCount > 0) {

                return Math.min(
                        90 + Math.min(
                                relevantFactChecks * 2,
                                8),
                        98);
            }

            if (mixedCount > 0) {
                return 65;
            }

            return 60;
        }

        // Related fact-check only
        if (relatedFactChecks > 0) {

            return Math.min(
                    35 + (relatedFactChecks * 5),
                    60);
        }

        // No fact-check evidence
        // Use supporting online sources
        int score = 0;

        score += Math.min(
                matchingArticles * 5,
                25);

        score += Math.min(
                uniqueSources * 15,
                60);

        return Math.min(score, 85);
    }

    // =========================================================
    // FACT-CHECK RATING CLASSIFICATION
    // =========================================================

    private String classifyRating(
            String rating) {

        String value =
                rating.toLowerCase().trim();

        if (value.contains("mostly false")
                || value.contains("false")
                || value.contains("incorrect")
                || value.contains("wrong")) {

            return "FALSE";
        }

        if (value.contains("misleading")) {

            return "MISLEADING";
        }

        if (value.contains("half true")
                || value.contains("partly true")
                || value.contains("partially true")
                || value.contains("mixed")) {

            return "MIXED";
        }

        if (value.contains("mostly true")
                || value.equals("true")
                || value.contains("accurate")
                || value.contains("correct")) {

            return "TRUE";
        }

        return "UNKNOWN";
    }

    // =========================================================
    // EDIT DISTANCE / DYNAMIC PROGRAMMING
    // =========================================================

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

                    int insert =
                            dp[i][j - 1];

                    int delete =
                            dp[i - 1][j];

                    int replace =
                            dp[i - 1][j - 1];

                    dp[i][j] =
                            1 + Math.min(
                                    insert,
                                    Math.min(
                                            delete,
                                            replace
                                    )
                            );
                }
            }
        }

        return dp[n][m];
    }

    private double calculateSimilarity(
            String first,
            String second) {

        if (first == null || second == null) {
            return 0;
        }

        int distance =
                editDistance(
                        first,
                        second);

        int maxLength =
                Math.max(
                        first.length(),
                        second.length());

        if (maxLength == 0) {
            return 100;
        }

        return (
                1 -
                ((double) distance / maxLength)
        ) * 100;
    }

    // =========================================================
    // JSON HELPERS
    // =========================================================

    private String getText(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()) {

            return "";
        }

        return value.asText();
    }

    private String getNestedText(
            JsonNode node,
            String parent,
            String child) {

        JsonNode parentNode =
                node.get(parent);

        if (parentNode == null
                || parentNode.isNull()) {

            return "";
        }

        JsonNode childNode =
                parentNode.get(child);

        if (childNode == null
                || childNode.isNull()) {

            return "";
        }

        return childNode.asText();
    }
}