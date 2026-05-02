package com.snippetmanager.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Snippet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String title;
    private String language;
    private List<String> tags;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Snippet() {
        this.id = UUID.randomUUID();
        this.title = "";
        this.language = "Text";
        this.tags = new ArrayList<>();
        this.code = "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = valueOrEmpty(title);
        touch();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = valueOrEmpty(language);
        touch();
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags == null ? List.of() : tags);
        touch();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = valueOrEmpty(code);
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    public String tagsAsCsv() {
        return String.join(", ", tags);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public String toString() {
        return title == null || title.isBlank() ? "Untitled Snippet" : title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Snippet snippet)) {
            return false;
        }
        return Objects.equals(id, snippet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
