package com.snippetmanager.persistence;

import com.snippetmanager.model.Snippet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SnippetRepository {
    private final Path storageFile;

    public SnippetRepository() {
        Path storageDir = Path.of(System.getProperty("user.home"), ".snippet-manager");
        this.storageFile = storageDir.resolve("snippets.ser");
    }

    public synchronized List<Snippet> findAll() {
        ensureStorageDirectory();
        if (!Files.exists(storageFile)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(storageFile))) {
            Object loaded = input.readObject();
            if (loaded instanceof List<?> rawList) {
                List<Snippet> snippets = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof Snippet snippet) {
                        snippets.add(snippet);
                    }
                }
                return snippets;
            }
            return new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load snippets from disk.", e);
        }
    }

    public synchronized Snippet save(Snippet snippet) {
        List<Snippet> snippets = findAll();
        boolean updated = false;

        for (int i = 0; i < snippets.size(); i++) {
            if (snippets.get(i).getId().equals(snippet.getId())) {
                snippets.set(i, snippet);
                updated = true;
                break;
            }
        }

        if (!updated) {
            snippets.add(snippet);
        }

        writeAll(snippets);
        return snippet;
    }

    public synchronized Optional<Snippet> findById(UUID id) {
        return findAll().stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public synchronized void delete(UUID id) {
        List<Snippet> snippets = findAll();
        snippets.removeIf(s -> s.getId().equals(id));
        writeAll(snippets);
    }

    private void writeAll(List<Snippet> snippets) {
        ensureStorageDirectory();
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(storageFile))) {
            output.writeObject(snippets);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save snippets to disk.", e);
        }
    }

    private void ensureStorageDirectory() {
        try {
            Files.createDirectories(storageFile.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory.", e);
        }
    }
}
