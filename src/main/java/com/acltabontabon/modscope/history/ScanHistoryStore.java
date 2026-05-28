package com.acltabontabon.modscope.history;

import com.acltabontabon.modscope.library.GameSource;
import com.acltabontabon.modscope.storage.AppPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-file-backed persistence for scan history.
 * Uses Jackson with only {@code Map<String,Object>} payloads, keeping the binary native-image friendly.
 */
public final class ScanHistoryStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path file;

    public ScanHistoryStore() {
        this(AppPaths.scanHistoryPath());
    }

    public ScanHistoryStore(Path file) {
        this.file = file;
    }

    public Path file() { return file; }

    @SuppressWarnings("unchecked")
    public ScanHistory load() {
        ScanHistory history = new ScanHistory();
        if (!Files.isRegularFile(file)) return history;
        try {
            Map<String, Object> root = MAPPER.readValue(file.toFile(), Map.class);
            List<Map<String, Object>> entries = (List<Map<String, Object>>) root.getOrDefault("entries", List.of());
            for (Map<String, Object> raw : entries) {
                history.upsert(fromMap(raw));
            }
        } catch (IOException | ClassCastException e) {
            // Corrupt file: return empty history instead of failing the app.
            return new ScanHistory();
        }
        return history;
    }

    public void save(ScanHistory history) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 1);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (ScanHistoryEntry e : history.all()) entries.add(toMap(e));
        root.put("entries", entries);
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), root);
    }

    public void record(ScanHistoryEntry entry) throws IOException {
        ScanHistory history = load();
        history.upsert(entry);
        save(history);
    }

    private static Map<String, Object> toMap(ScanHistoryEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gameId", e.gameId());
        m.put("displayName", e.displayName());
        m.put("installPath", e.installPath());
        m.put("source", e.source().name());
        m.put("lastScanAt", e.lastScanAt());
        m.put("lastScanMode", e.lastScanMode());
        m.put("engineHint", e.engineHint());
        m.put("surfaceScore", e.surfaceScore());
        m.put("reportPath", e.reportPath());
        m.put("recommendedAction", e.recommendedAction());
        m.put("badges", e.badges());
        m.put("warnings", e.warnings());
        return m;
    }

    @SuppressWarnings("unchecked")
    private static ScanHistoryEntry fromMap(Map<String, Object> m) {
        return new ScanHistoryEntry(
            str(m.get("gameId")),
            str(m.get("displayName")),
            str(m.get("installPath")),
            parseSource(str(m.get("source"))),
            str(m.get("lastScanAt")),
            str(m.get("lastScanMode")),
            str(m.get("engineHint")),
            str(m.get("surfaceScore")),
            str(m.get("reportPath")),
            str(m.get("recommendedAction")),
            (List<String>) m.getOrDefault("badges", List.of()),
            (List<String>) m.getOrDefault("warnings", List.of())
        );
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static GameSource parseSource(String s) {
        if (s == null) return GameSource.UNKNOWN;
        try { return GameSource.valueOf(s); } catch (IllegalArgumentException e) { return GameSource.UNKNOWN; }
    }
}
