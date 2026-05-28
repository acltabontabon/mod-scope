package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.history.ScanHistoryEntry;
import com.acltabontabon.modscope.history.ScanHistoryStore;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.report.library.LibrarySummaryMarkdownWriter;
import com.acltabontabon.modscope.storage.AppPaths;
import com.acltabontabon.modscope.triage.Badge;
import com.acltabontabon.modscope.triage.GameTriageResult;
import com.acltabontabon.modscope.triage.QuickTriageService;
import com.acltabontabon.modscope.tui.TuiState;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Side-effect helper: persists a triage outcome into scan-history + library-summary. */
public final class TriagePersistence {

    private TriagePersistence() {}

    public static void record(TuiState state, DetectedGame game, QuickTriageService.TriageOutcome outcome) {
        if (!outcome.success()) return;
        GameTriageResult r = outcome.result();
        ScanHistoryEntry entry = new ScanHistoryEntry(
            game.id(),
            game.displayName(),
            String.valueOf(game.installPath()),
            game.source(),
            Instant.now().toString(),
            "QUICK",
            r.detectedEngine().name(),
            surfaceFromBadges(r.badges()),
            AppPaths.reportsForGame(game.safeId()).toString(),
            r.recommendedAction().name(),
            r.badges().stream().map(Badge::name).toList(),
            List.of()
        );
        try {
            ScanHistoryStore store = new ScanHistoryStore();
            store.record(entry);
            state.scanHistory = store.load();
            LibrarySummaryMarkdownWriter.write(AppPaths.librarySummaryPath(), state.detectedGames, state.scanHistory);
        } catch (IOException ignored) {
            // Persistence failure is non-fatal; the UI still shows the cached triage result.
        }
    }

    private static String surfaceFromBadges(List<Badge> badges) {
        if (badges.contains(Badge.GOOD_FIRST_TARGET)) return "HIGH";
        if (badges.contains(Badge.HARD_TARGET)) return "LOW";
        if (badges.contains(Badge.LOOSE_FILES) || badges.contains(Badge.CONFIGS_FOUND)) return "MEDIUM";
        return "LOW";
    }
}
