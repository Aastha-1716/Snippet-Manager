package com.snippetmanager.persistence;

import com.snippetmanager.model.Snippet;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NeonSnippetRepository {
    private final DataSource ds;

    public NeonSnippetRepository(DataSource ds) {
        this.ds = ds;
    }

    public List<Snippet> findAll() {
        List<Snippet> list = new ArrayList<>();
        String sql = "SELECT id, title, language, tags, code, created_at, updated_at FROM snippets ORDER BY updated_at DESC";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Snippet s = mapRow(rs);
                    list.add(s);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read snippets from Neon: " + e.getMessage(), e);
        }
        return list;
    }

    public synchronized Snippet save(Snippet snippet) {
        String update = "UPDATE snippets SET title=?, language=?, tags=?, code=?, updated_at=? WHERE id=?";
        String insert = "INSERT INTO snippets (id, title, language, tags, code, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(update)) {
                ps.setString(1, snippet.getTitle());
                ps.setString(2, snippet.getLanguage());
                Array arr = c.createArrayOf("text", snippet.getTags().toArray(new String[0]));
                ps.setArray(3, arr);
                ps.setString(4, snippet.getCode());
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.setObject(6, snippet.getId());
                int updated = ps.executeUpdate();
                if (updated > 0) return snippet;
            }

            try (PreparedStatement ps2 = c.prepareStatement(insert)) {
                ps2.setObject(1, snippet.getId());
                ps2.setString(2, snippet.getTitle());
                ps2.setString(3, snippet.getLanguage());
                Array arr2 = c.createArrayOf("text", snippet.getTags().toArray(new String[0]));
                ps2.setArray(4, arr2);
                ps2.setString(5, snippet.getCode());
                ps2.setTimestamp(6, Timestamp.valueOf(snippet.getCreatedAt()));
                ps2.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                ps2.executeUpdate();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save snippet to Neon: " + e.getMessage(), e);
        }
        return snippet;
    }

    public Optional<Snippet> findById(UUID id) {
        String sql = "SELECT id, title, language, tags, code, created_at, updated_at FROM snippets WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to query snippet: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM snippets WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete snippet: " + e.getMessage(), e);
        }
    }

    private Snippet mapRow(ResultSet rs) throws Exception {
        Snippet s = new Snippet();
        s.setId((UUID) rs.getObject("id"));
        s.setTitle(rs.getString("title"));
        s.setLanguage(rs.getString("language"));
        Array arr = rs.getArray("tags");
        if (arr != null) {
            Object[] o = (Object[]) arr.getArray();
            List<String> tags = new ArrayList<>();
            for (Object it : o) tags.add(it == null ? "" : it.toString());
            s.setTags(tags);
        } else {
            s.setTags(List.of());
        }
        s.setCode(rs.getString("code"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ca != null) s.setCreatedAt(ca.toLocalDateTime());
        if (ua != null) s.setUpdatedAt(ua.toLocalDateTime());
        return s;
    }

    /**
     * Seed DB with a few classic DSA snippets when table empty.
     */
    public void seedIfEmpty() {
        String countSql = "SELECT count(1) FROM snippets";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(countSql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getLong(1) > 0) return;
            }
        } catch (Exception e) {
            // ignore
            return;
        }

        // insert a few seeds
        Snippet s1 = new Snippet();
        s1.setTitle("Binary Search (iterative)");
        s1.setLanguage("Java");
        s1.setTags(List.of("search", "binary-search", "array", "sorting"));
        s1.setCode("public static int binarySearch(int[] a, int x) { int lo=0, hi=a.length-1; while(lo<=hi){int mid=(lo+hi)/2; if(a[mid]==x) return mid; if(a[mid] < x) lo = mid+1; else hi = mid-1;} return -1;} ");

        Snippet s2 = new Snippet();
        s2.setTitle("DFS recursive (graph)");
        s2.setLanguage("Java");
        s2.setTags(List.of("graph", "dfs", "traversal"));
        s2.setCode("void dfs(int u, boolean[] vis, List<Integer>[] g){ vis[u]=true; for(int v: g[u]) if(!vis[v]) dfs(v, vis, g); }");

        save(s1);
        save(s2);
    }
}
