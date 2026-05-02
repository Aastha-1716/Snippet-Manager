package com.snippetmanager.persistence;

import com.snippetmanager.model.Snippet;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RailwayMySqlSnippetRepository {
    private final DataSource dataSource;

    public RailwayMySqlSnippetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        ensureSchema();
    }

    public List<Snippet> findAll() {
        List<Snippet> snippets = new ArrayList<>();
        String sql = "SELECT id, title, language, tags, code, created_at, updated_at FROM snippets ORDER BY updated_at DESC";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                snippets.add(mapRow(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read snippets from Railway MySQL: " + e.getMessage(), e);
        }
        return snippets;
    }

    public synchronized Snippet save(Snippet snippet) {
        String sql = "INSERT INTO snippets (id, title, language, tags, code, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE title = VALUES(title), language = VALUES(language), tags = VALUES(tags), code = VALUES(code), updated_at = VALUES(updated_at)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, snippet.getId().toString());
            statement.setString(2, snippet.getTitle());
            statement.setString(3, snippet.getLanguage());
            statement.setString(4, String.join("\n", snippet.getTags()));
            statement.setString(5, snippet.getCode());
            statement.setTimestamp(6, Timestamp.valueOf(snippet.getCreatedAt()));
            statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save snippet to Railway MySQL: " + e.getMessage(), e);
        }
        return snippet;
    }

    public Optional<Snippet> findById(UUID id) {
        String sql = "SELECT id, title, language, tags, code, created_at, updated_at FROM snippets WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to query snippet from Railway MySQL: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM snippets WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete snippet from Railway MySQL: " + e.getMessage(), e);
        }
    }

    public void seedIfEmpty() {
        String countSql = "SELECT count(1) FROM snippets";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(countSql); ResultSet rs = statement.executeQuery()) {
            if (rs.next() && rs.getLong(1) > 0) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        Snippet binarySearch = new Snippet();
        binarySearch.setTitle("Binary Search (iterative)");
        binarySearch.setLanguage("Java");
        binarySearch.setTags(List.of("search", "binary-search", "array", "sorting"));
        binarySearch.setCode("public static int binarySearch(int[] a, int x) { int lo = 0, hi = a.length - 1; while (lo <= hi) { int mid = (lo + hi) / 2; if (a[mid] == x) return mid; if (a[mid] < x) lo = mid + 1; else hi = mid - 1; } return -1; }");

        Snippet dfs = new Snippet();
        dfs.setTitle("DFS recursive (graph)");
        dfs.setLanguage("Java");
        dfs.setTags(List.of("graph", "dfs", "traversal"));
        dfs.setCode("void dfs(int u, boolean[] vis, List<Integer>[] g) { vis[u] = true; for (int v : g[u]) if (!vis[v]) dfs(v, vis, g); }");

        save(binarySearch);
        save(dfs);
    }

    private void ensureSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS snippets (" +
                "id CHAR(36) NOT NULL PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "language VARCHAR(100) NOT NULL, " +
                "tags TEXT, " +
                "code LONGTEXT NOT NULL, " +
                "created_at DATETIME(6) NOT NULL, " +
                "updated_at DATETIME(6) NOT NULL" +
                ")";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Railway MySQL schema: " + e.getMessage(), e);
        }
    }

    private Snippet mapRow(ResultSet rs) throws Exception {
        Snippet snippet = new Snippet();
        snippet.setId(UUID.fromString(rs.getString("id")));
        snippet.setTitle(rs.getString("title"));
        snippet.setLanguage(rs.getString("language"));
        String tags = rs.getString("tags");
        if (tags == null || tags.isBlank()) {
            snippet.setTags(List.of());
        } else {
            String[] split = tags.split("\\R");
            List<String> parsed = new ArrayList<>();
            for (String tag : split) {
                if (!tag.isBlank()) {
                    parsed.add(tag.trim());
                }
            }
            snippet.setTags(parsed);
        }
        snippet.setCode(rs.getString("code"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) {
            snippet.setCreatedAt(createdAt.toLocalDateTime());
        }
        if (updatedAt != null) {
            snippet.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return snippet;
    }
}