package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.history.ScanHistoryEntry;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.recommendation.Recommendation;
import com.acltabontabon.modscope.storage.AppPaths;
import com.acltabontabon.modscope.triage.GameTriageResult;
import com.acltabontabon.modscope.triage.QuickTriageService;
import com.acltabontabon.modscope.tui.TuiScreen;
import com.acltabontabon.modscope.tui.TuiState;
import com.acltabontabon.modscope.tui.components.BadgeBar;
import com.acltabontabon.modscope.tui.components.KeyValueList;
import com.acltabontabon.modscope.tui.components.StatusPanel;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GameDetailsScreen {

    private GameDetailsScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        DetectedGame game = state.selectedGame;
        if (game == null) {
            state.screen = TuiScreen.HOME;
            return true;
        }
        return switch (event) {
            case KeyEvent k when k.isCancel() -> { state.screen = TuiScreen.HOME; yield true; }
            case KeyEvent k when k.isQuit()   -> { runner.quit(); yield false; }
            case KeyEvent k when keyChar(k, 't') -> { runQuickTriage(state, runner, game); yield true; }
            case KeyEvent k when keyChar(k, 'd') -> {
                // Hand off to existing scan setup
                state.setupGameName = game.displayName();
                state.setupGameDir = game.installPath().toString();
                state.setupProfileId = null;
                state.setupOutputDir = AppPaths.reportsForGame(game.safeId()).toString();
                state.screen = TuiScreen.SCAN_SETUP;
                yield true;
            }
            case KeyEvent k when keyChar(k, 'r') -> {
                openLastReport(state, game);
                yield true;
            }
            case KeyEvent k when keyChar(k, 'c') -> {
                Optional.ofNullable(state.triageCache.get(game.id())).ifPresent(t -> {
                    state.currentRecommendations = state.scanHistory.find(game.id())
                        .map(e -> List.<Recommendation>of()).orElse(List.of());
                });
                state.screen = TuiScreen.RECOMMENDATIONS;
                yield true;
            }
            default -> false;
        };
    }

    private static boolean keyChar(KeyEvent k, char c) {
        String r = k.toString();
        return r.length() == 1 && Character.toLowerCase(r.charAt(0)) == Character.toLowerCase(c);
    }

    private static void runQuickTriage(TuiState state, TuiRunner runner, DetectedGame game) {
        if (state.quickTriageService == null) return;
        Thread.ofVirtual().start(() -> {
            try {
                QuickTriageService.TriageOutcome outcome = state.quickTriageService.triage(game);
                runner.runOnRenderThread(() -> {
                    if (outcome.success()) {
                        state.triageCache.put(game.id(), outcome.result());
                        state.currentRecommendations = outcome.recommendations();
                        TriagePersistence.record(state, game, outcome);
                    } else {
                        state.scanAllFailures.add(game.displayName() + ": " + outcome.error());
                    }
                });
            } catch (Exception e) {
                runner.runOnRenderThread(() ->
                    state.scanAllFailures.add(game.displayName() + ": " + e.getMessage()));
            }
        });
    }

    private static void openLastReport(TuiState state, DetectedGame game) {
        java.nio.file.Path report = AppPaths.reportsForGame(game.safeId()).resolve("scan-summary.md");
        if (java.nio.file.Files.isRegularFile(report)) {
            state.reportViewerPath = report;
            try {
                state.reportViewerLines = java.nio.file.Files.readAllLines(report);
            } catch (java.io.IOException e) {
                state.reportViewerLines = List.of("Failed to read report: " + e.getMessage());
            }
            state.reportViewerScroll = 0;
            state.screen = TuiScreen.REPORT_VIEWER;
        }
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();
        DetectedGame game = state.selectedGame;
        if (game == null) return;

        Block outer = Block.builder()
            .title(Title.from(" " + game.displayName() + " "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outer.inner(area);
        frame.renderWidget(outer, area);

        int y = inner.y();
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  READ-ONLY — no game files will be modified.", Style.EMPTY.fg(Color.GREEN))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );
        y++;

        GameTriageResult triage = state.triageCache.get(game.id());
        ScanHistoryEntry hist = state.scanHistory.find(game.id()).orElse(null);

        List<KeyValueList.Row> rows = new ArrayList<>();
        rows.add(KeyValueList.Row.of("Source:",      game.source().name(), Color.CYAN));
        rows.add(KeyValueList.Row.of("App ID:",      game.appId() == null ? "—" : game.appId()));
        rows.add(KeyValueList.Row.of("Install path:", String.valueOf(game.installPath())));
        rows.add(KeyValueList.Row.of("Executables:", game.executableCandidates().isEmpty()
            ? "none detected"
            : game.executableCandidates().size() + " candidate(s)"));
        if (triage != null) {
            rows.add(KeyValueList.Row.of("Engine:", triage.detectedEngine() + " (" + triage.engineConfidence() + "%)", Color.MAGENTA));
            rows.add(KeyValueList.Row.of("Confidence:", triage.scanConfidence() + "%"));
            rows.add(KeyValueList.Row.of("Archive formats:", triage.archiveFormats().isEmpty() ? "—" : String.join(" ", triage.archiveFormats())));
            rows.add(KeyValueList.Row.of("Configs:", triage.configSurface().name()));
            rows.add(KeyValueList.Row.of("Saves:", triage.saveSurface().name()));
            rows.add(KeyValueList.Row.of("Loose files:", triage.looseFileSurface().name()));
            rows.add(KeyValueList.Row.of("Packed archives:", triage.packedArchiveSurface().name()));
            rows.add(KeyValueList.Row.of("External tool:", triage.externalToolDependency().name()));
            rows.add(KeyValueList.Row.of("Recommended:", triage.recommendedAction().name(), Color.YELLOW));
        } else if (hist != null) {
            rows.add(KeyValueList.Row.of("Last scan:", hist.lastScanAt()));
            rows.add(KeyValueList.Row.of("Engine:", hist.engineHint(), Color.MAGENTA));
            rows.add(KeyValueList.Row.of("Surface:", hist.surfaceScore()));
            rows.add(KeyValueList.Row.of("Recommended:", hist.recommendedAction(), Color.YELLOW));
        } else {
            rows.add(KeyValueList.Row.of("Triage status:", "Not yet triaged. Press t to run a quick triage.", Color.YELLOW));
        }

        int rowsHeight = Math.min(rows.size(), inner.height() - (y - inner.y()) - 4);
        Rect kvArea = Rect.of(new Position(inner.x(), y), new Size(inner.width(), rowsHeight));
        KeyValueList.render(frame, kvArea, rows);
        y += rowsHeight;

        if (triage != null && !triage.badges().isEmpty()) {
            BadgeBar.render(frame, Rect.of(new Position(inner.x(), y), new Size(inner.width(), 1)), triage.badges());
            y++;
        }

        StatusPanel.render(frame, inner, "t triage   d deep scan   r open report   c recommendations   Esc back   q quit");
    }
}
