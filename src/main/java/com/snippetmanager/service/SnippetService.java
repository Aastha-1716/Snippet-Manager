package com.snippetmanager.service;

import com.snippetmanager.model.Snippet;
import com.snippetmanager.persistence.SnippetRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class SnippetService {
    private final SnippetRepository repository;

    public SnippetService(SnippetRepository repository) {
        this.repository = repository;
    }

    public List<Snippet> getAll() {
        List<Snippet> snippets = new ArrayList<>(repository.findAll());
        snippets.sort(Comparator.comparing(Snippet::getUpdatedAt).reversed());
        return snippets;
    }

    public List<Snippet> search(String query, String languageFilter, String tagFilter) {
        String normalizedQuery = normalize(query);
        String normalizedLanguage = normalize(languageFilter);
        String normalizedTag = normalize(tagFilter);

        List<Snippet> matches = new ArrayList<>();
        for (Snippet snippet : getAll()) {
            if (!matchesLanguage(snippet, normalizedLanguage)) {
                continue;
            }
            if (!matchesTag(snippet, normalizedTag)) {
                continue;
            }
            if (!matchesQuery(snippet, normalizedQuery)) {
                continue;
            }
            matches.add(snippet);
        }
        return matches;
    }

    public Snippet save(Snippet snippet) {
        return repository.save(snippet);
    }

    public Optional<Snippet> getById(UUID id) {
        return repository.findById(id);
    }

    public void delete(UUID id) {
        repository.delete(id);
    }

    private boolean matchesLanguage(Snippet snippet, String language) {
        if (language.isBlank() || "all".equals(language)) {
            return true;
        }
        return normalize(snippet.getLanguage()).equals(language);
    }

    private boolean matchesTag(Snippet snippet, String tag) {
        if (tag.isBlank()) {
            return true;
        }
        for (String item : snippet.getTags()) {
            if (normalize(item).contains(tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesQuery(Snippet snippet, String query) {
        if (query.isBlank()) {
            return true;
        }
        if (normalize(snippet.getTitle()).contains(query)) {
            return true;
        }
        if (normalize(snippet.getLanguage()).contains(query)) {
            return true;
        }
        if (normalize(snippet.getCode()).contains(query)) {
            return true;
        }
        for (String tag : snippet.getTags()) {
            if (normalize(tag).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
