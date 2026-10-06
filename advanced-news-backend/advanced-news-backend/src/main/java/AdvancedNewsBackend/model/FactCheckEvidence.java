package com.advancednews.model;

public class FactCheckEvidence {

    private String claim;
    private String publisher;
    private String rating;
    private String title;
    private String url;

    private double matchScore;
    private String matchType;

    public FactCheckEvidence() {
    }

    public FactCheckEvidence(
            String claim,
            String publisher,
            String rating,
            String title,
            String url,
            double matchScore,
            String matchType) {

        this.claim = claim;
        this.publisher = publisher;
        this.rating = rating;
        this.title = title;
        this.url = url;
        this.matchScore = matchScore;
        this.matchType = matchType;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }
}