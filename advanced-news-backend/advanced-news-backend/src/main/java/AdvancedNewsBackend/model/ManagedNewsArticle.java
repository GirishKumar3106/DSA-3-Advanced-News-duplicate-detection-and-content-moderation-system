package com.advancednews.model;

public class ManagedNewsArticle {

    private int id;
    private String title;
    private String content;
    private String category;
    private String moderationStatus;

    public ManagedNewsArticle() {
    }

    public ManagedNewsArticle(
            int id,
            String title,
            String content,
            String category,
            String moderationStatus) {

        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.moderationStatus = moderationStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(String moderationStatus) {
        this.moderationStatus = moderationStatus;
    }
}
