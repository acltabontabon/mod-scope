package com.acltabontabon.modscope.report.library;

import com.acltabontabon.modscope.history.ScanHistory;
import com.acltabontabon.modscope.history.ScanHistoryEntry;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.library.GameSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LibrarySummaryMarkdownWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void aggregatesEntriesIntoMarkdownSections() throws IOException {
        var history = new ScanHistory();
        history.upsert(entry("steam-1", "Config Game", "INSPECT_CONFIGS", List.of("CONFIGS_FOUND", "GOOD_FIRST_TARGET")));
        history.upsert(entry("steam-2", "Archive Game", "INSPECT_ARCHIVES", List.of("ARCHIVE_HEAVY", "EXTERNAL_TOOL_NEEDED")));
        history.upsert(entry("steam-3", "Saves Game", "BUILD_SAVE_BACKUP", List.of("SAVES_FOUND")));
        history.upsert(entry("steam-4", "Mystery Game", "MANUAL_REVIEW", List.of("UNKNOWN_ENGINE")));

        var detected = List.of(
            game("steam-1", "Config Game"),
            game("steam-2", "Archive Game"),
            game("steam-3", "Saves Game"),
            game("steam-4", "Mystery Game"),
            game("steam-5", "Unscanned Game")
        );

        Path out = tempDir.resolve("library-summary.md");
        LibrarySummaryMarkdownWriter.write(out, detected, history);

        String md = Files.readString(out);
        assertTrue(md.contains("Detected games:** 5"));
        assertTrue(md.contains("Games with a recorded scan:** 4"));
        assertTrue(md.contains("Top modding candidates"));
        assertTrue(md.contains("Config Game"));
        assertTrue(md.contains("Archive-heavy games"));
        assertTrue(md.contains("Archive Game"));
        assertTrue(md.contains("Games with saves detected"));
        assertTrue(md.contains("Saves Game"));
        assertTrue(md.contains("Unknown / manual-review games"));
        assertTrue(md.contains("Mystery Game"));
        assertTrue(md.contains("Detected but not yet scanned"));
        assertTrue(md.contains("Unscanned Game"));
        assertTrue(md.contains("READ-ONLY SCAN"));
    }

    private static ScanHistoryEntry entry(String id, String name, String rec, List<String> badges) {
        return new ScanHistoryEntry(
            id, name, "/tmp/" + id, GameSource.STEAM, "2026-01-01T00:00:00Z",
            "QUICK", "UNREAL", "MEDIUM", "/tmp/r", rec, badges, List.of()
        );
    }

    private static DetectedGame game(String id, String name) {
        return new DetectedGame(id, name, GameSource.STEAM, "0", Path.of("/tmp/" + id),
            List.of(), null, Instant.now(), 80, Map.of());
    }
}
