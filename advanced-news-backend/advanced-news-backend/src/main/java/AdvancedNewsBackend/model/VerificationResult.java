package com.advancednews.model;

import java.util.List;

public class VerificationResult {

    private String claim;
    private String verdict;
    private int confidence;

    private int matchingArticles;
    private int uniqueSources;

    private int factChecksReturned;
    private int relevantFactChecks;
    private int relatedFactChecks;

    private List<String> sources;
    private List<FactCheckEvidence> factChecks;

    public VerificationResult() {
    }

    public VerificationResult(
            String claim,
            String verdict,
            int confidence,
            int matchingArticles,
            int uniqueSources,
            int factChecksReturned,
            int relevantFactChecks,
            int relatedFactChecks,
            List<String> sources,
            List<FactCheckEvidence> factChecks) {

        this.claim = claim;
        this.verdict = verdict;
        this.confidence = confidence;
        this.matchingArticles = matchingArticles;
        this.uniqueSources = uniqueSources;
        this.factChecksReturned = factChecksReturned;
        this.relevantFactChecks = relevantFactChecks;
        this.relatedFactChecks = relatedFactChecks;
        this.sources = sources;
        this.factChecks = factChecks;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public int getMatchingArticles() {
        return matchingArticles;
    }

    public void setMatchingArticles(int matchingArticles) {
        this.matchingArticles = matchingArticles;
    }

    public int getUniqueSources() {
        return uniqueSources;
    }

    public void setUniqueSources(int uniqueSources) {
        this.uniqueSources = uniqueSources;
    }

    public int getFactChecksReturned() {
        return factChecksReturned;
    }

    public void setFactChecksReturned(int factChecksReturned) {
        this.factChecksReturned = factChecksReturned;
    }

    public int getRelevantFactChecks() {
        return relevantFactChecks;
    }

    public void setRelevantFactChecks(int relevantFactChecks) {
        this.relevantFactChecks = relevantFactChecks;
    }

    public int getRelatedFactChecks() {
        return relatedFactChecks;
    }

    public void setRelatedFactChecks(int relatedFactChecks) {
        this.relatedFactChecks = relatedFactChecks;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<FactCheckEvidence> getFactChecks() {
        return factChecks;
    }

    public void setFactChecks(List<FactCheckEvidence> factChecks) {
        this.factChecks = factChecks;
    }
}