package com.acltabontabon.modscope.report.library;

import com.acltabontabon.modscope.history.ScanHistory;
import com.acltabontabon.modscope.history.ScanHistoryEntry;
import com.acltabontabon.modscope.library.DetectedGame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Aggregates scan-history into a single library-level markdown report.
 * Used by the dashboard's "Open recent reports" action and updated after every triage.
 */
public final class LibrarySummaryMarkdownWriter {

    private LibrarySummaryMarkdownWriter() {}

    public static void write(Path outputFile, List<DetectedGame> detected, ScanHistory history) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ModScope library summary\n\n");
        sb.append("> READ-ONLY SCAN — ModScope did not modify, rename, delete, patch, overwrite, inject, or execute game files.\n\n");

        sb.append("- **Detected games:** ").append(detected.size()).append('\n');
        sb.append("- **Games with a recorded scan:** ").append(history.size()).append('\n');
        sb.append('\n');

        List<ScanHistoryEntry> all = history.all();

        section(sb, "Top modding candidates", all, e ->
            "GOOD_FIRST_MOD_TARGET".equals(e.recommendedAction())
                || (e.badges() != null && e.badges().contains("GOOD_FIRST_TARGET")));

        section(sb, "Archive-heavy games", all, e ->
            e.badges() != null && e.badges().contains("ARCHIVE_HEAVY"));

        section(sb, "Config-friendly games", all, e ->
            e.badges() != null && e.badges().contains("CONFIGS_FOUND"));

        section(sb, "Games with saves detected", all, e ->
            e.badges() != null && e.badges().contains("SAVES_FOUND"));

        section(sb, "Games likely needing external tools", all, e ->
            e.badges() != null && e.badges().contains("EXTERNAL_TOOL_NEEDED"));

        section(sb, "Unknown / manual-review games", all, e ->
            e.badges() != null && e.badges().contains("UNKNOWN_ENGINE"));

        sb.append("## Detected but not yet scanned\n\n");
        long unscanned = detected.stream()
            .filter(g -> history.find(g.id()).isEmpty())
            .count();
        if (unscanned == 0) {
            sb.append("None — every detected game has been triaged.\n\n");
        } else {
            detected.stream()
                .filter(g -> history.find(g.id()).isEmpty())
                .forEach(g -> sb.append("- ").append(g.displayName())
                    .append(" — `").append(g.installPath()).append("`\n"));
            sb.append('\n');
        }

        sb.append("---\n*Use read-only listing first for any archive recommendation. Do not extract or rebuild archives without backups.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void section(StringBuilder sb, String title, List<ScanHistoryEntry> all,
                                java.util.function.Predicate<ScanHistoryEntry> filter) {
        sb.append("## ").append(title).append("\n\n");
        List<ScanHistoryEntry> matching = all.stream().filter(filter).toList();
        if (matching.isEmpty()) {
            sb.append("None.\n\n");
            return;
        }
        for (ScanHistoryEntry e : matching) {
            sb.append("- **").append(orUnknown(e.displayName())).append("** — ");
            sb.append(orDash(e.engineHint())).append(" · ");
            sb.append("surface ").append(orDash(e.surfaceScore())).append(" · ");
            sb.append("next: ").append(orDash(e.recommendedAction())).append('\n');
        }
        sb.append('\n');
    }

    private static String orUnknown(String s) { return Optional.ofNullable(s).filter(v -> !v.isBlank()).orElse("Unknown"); }
    private static String orDash(String s) { return Optional.ofNullable(s).filter(v -> !v.isBlank()).orElse("—"); }
}
