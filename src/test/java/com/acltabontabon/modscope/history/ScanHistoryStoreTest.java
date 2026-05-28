package com.acltabontabon.modscope.history;

import com.acltabontabon.modscope.library.GameSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScanHistoryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripsEntry() throws IOException {
        var store = new ScanHistoryStore(tempDir.resolve("scan-history.json"));
        var entry = entry("steam-1", "First Light");
        store.record(entry);

        var loaded = store.load();
        assertEquals(1, loaded.size());
        assertEquals("First Light", loaded.find("steam-1").orElseThrow().displayName());
    }

    @Test
    void upsertsExistingEntryByGameId() throws IOException {
        var store = new ScanHistoryStore(tempDir.resolve("scan-history.json"));
        store.record(entry("steam-1", "Original"));
        store.record(entry("steam-1", "Renamed"));

        var loaded = store.load();
        assertEquals(1, loaded.size());
        assertEquals("Renamed", loaded.find("steam-1").orElseThrow().displayName());
    }

    @Test
    void missingFileReturnsEmpty() {
        var store = new ScanHistoryStore(tempDir.resolve("does-not-exist.json"));
        assertEquals(0, store.load().size());
    }

    @Test
    void corruptFileFallsBackToEmpty() throws IOException {
        Path f = tempDir.resolve("scan-history.json");
        Files.writeString(f, "{ this is not valid json");
        var store = new ScanHistoryStore(f);
        assertEquals(0, store.load().size());
    }

    private static ScanHistoryEntry entry(String id, String name) {
        return new ScanHistoryEntry(
            id, name, "/tmp/g", GameSource.STEAM,
            "2026-05-29T00:00:00Z", "QUICK", "UNREAL", "MEDIUM",
            "/tmp/report", "INSPECT_CONFIGS",
            List.of("CONFIGS_FOUND"), List.of()
        );
    }
}
