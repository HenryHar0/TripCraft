package com.henry.tripcraft;

public class BlogPost {
    private String title;
    private String description;
    private String author;
    private String date;
    private String imageColor;
    private String content;
    private int imageResourceId; // New field for image resource

    // Constructor with image resource
    public BlogPost(String title, String description, String author, String date, String imageColor, String content, int imageResourceId) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.date = date;
        this.imageColor = imageColor;
        this.content = content;
        this.imageResourceId = imageResourceId;
    }

    // Constructor without image resource (backward compatibility)
    public BlogPost(String title, String description, String author, String date, String imageColor, String content) {
        this(title, description, author, date, imageColor, content, 0);
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public String getImageColor() {
        return imageColor;
    }

    public String getContent() {
        return content;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setImageColor(String imageColor) {
        this.imageColor = imageColor;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }
}