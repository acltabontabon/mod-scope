package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.history.ScanHistoryEntry;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.storage.AppPaths;
import com.acltabontabon.modscope.triage.GameTriageResult;
import com.acltabontabon.modscope.triage.QuickTriageService;
import com.acltabontabon.modscope.tui.TuiScreen;
import com.acltabontabon.modscope.tui.TuiState;
import com.acltabontabon.modscope.tui.components.BadgeBar;
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
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.List;
import java.util.Optional;

/** Installed-games dashboard. Lists every detected game with badges and a recommended next action. */
public final class HomeScreen {

    /** "Scan a different folder" and "Exit" are appended after the game list. */
    private static final int TAIL_ITEMS = 4;

    private HomeScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        int gameCount = state.detectedGames.size();
        int total = gameCount + TAIL_ITEMS;
        return switch (event) {
            case KeyEvent k when k.isDown()   -> { state.homeList.selectNext(total); yield true; }
            case KeyEvent k when k.isUp()     -> { state.homeList.selectPrevious(); yield true; }
            case KeyEvent k when k.isSelect() -> handleSelection(state.homeList.selected() == null ? 0 : state.homeList.selected(), runner, state);
            case KeyEvent k when k.isQuit()   -> { runner.quit(); yield false; }
            case KeyEvent k when isChar(k, 't') -> { triageSelected(state, runner); yield true; }
            case KeyEvent k when isChar(k, 'T') -> { triageAll(state, runner); yield true; }
            case KeyEvent k when isChar(k, 's') -> { state.screen = TuiScreen.SETTINGS; yield true; }
            case KeyEvent k when isChar(k, 'r') -> { openLibrarySummary(state); yield true; }
            case KeyEvent k when isChar(k, 'd') -> { deepScanSelected(state); yield true; }
            default -> false;
        };
    }

    private static boolean isChar(KeyEvent k, char c) {
        String r = k.toString();
        return r.length() == 1 && r.charAt(0) == c;
    }

    private static boolean handleSelection(int index, TuiRunner runner, TuiState state) {
        List<DetectedGame> games = state.detectedGames;
        if (index < games.size()) {
            state.selectedGame = games.get(index);
            state.screen = TuiScreen.GAME_DETAILS;
            return true;
        }
        int tail = index - games.size();
        switch (tail) {
            case 0 -> triageAll(state, runner);
            case 1 -> openLibrarySummary(state);
            case 2 -> {
                state.setupGameName = "";
                state.setupGameDir = "";
                state.setupProfileId = null;
                state.scanResult = null;
                state.scanError = null;
                state.scanStarted = false;
                state.scanLog.clear();
                state.screen = TuiScreen.SCAN_SETUP;
            }
            case 3 -> runner.quit();
        }
        return true;
    }

    private static void triageSelected(TuiState state, TuiRunner runner) {
        Integer sel = state.homeList.selected();
        if (sel == null || sel >= state.detectedGames.size()) return;
        DetectedGame game = state.detectedGames.get(sel);
        if (state.quickTriageService == null) return;
        Thread.ofVirtual().start(() -> {
            QuickTriageService.TriageOutcome outcome = state.quickTriageService.triage(game);
            runner.runOnRenderThread(() -> {
                if (outcome.success()) {
                    state.triageCache.put(game.id(), outcome.result());
                    TriagePersistence.record(state, game, outcome);
                } else {
                    state.scanAllFailures.add(game.displayName() + ": " + outcome.error());
                }
            });
        });
    }

    private static void triageAll(TuiState state, TuiRunner runner) {
        if (state.quickTriageService == null || state.scanAllQuickActive) return;
        state.scanAllQuickActive = true;
        state.scanAllTotal = state.detectedGames.size();
        state.scanAllCompleted = 0;
        state.scanAllFailures.clear();
        Thread.ofVirtual().start(() -> {
            for (DetectedGame game : state.detectedGames) {
                runner.runOnRenderThread(() -> state.scanAllCurrentGame = game.displayName());
                try {
                    QuickTriageService.TriageOutcome outcome = state.quickTriageService.triage(game);
                    runner.runOnRenderThread(() -> {
                        if (outcome.success()) {
                            state.triageCache.put(game.id(), outcome.result());
                            TriagePersistence.record(state, game, outcome);
                        } else {
                            state.scanAllFailures.add(game.displayName() + ": " + outcome.error());
                        }
                        state.scanAllCompleted++;
                    });
                } catch (Exception e) {
                    runner.runOnRenderThread(() -> {
                        state.scanAllFailures.add(game.displayName() + ": " + e.getMessage());
                        state.scanAllCompleted++;
                    });
                }
            }
            runner.runOnRenderThread(() -> state.scanAllQuickActive = false);
        });
    }

    private static void openLibrarySummary(TuiState state) {
        java.nio.file.Path summary = AppPaths.librarySummaryPath();
        state.reportViewerPath = summary;
        if (java.nio.file.Files.isRegularFile(summary)) {
            try { state.reportViewerLines = java.nio.file.Files.readAllLines(summary); }
            catch (java.io.IOException e) { state.reportViewerLines = List.of("Failed to read: " + e.getMessage()); }
        } else {
            state.reportViewerLines = List.of("No library summary yet — press T to scan all games quickly.");
        }
        state.reportViewerScroll = 0;
        state.screen = TuiScreen.REPORT_VIEWER;
    }

    private static void deepScanSelected(TuiState state) {
        Integer sel = state.homeList.selected();
        if (sel == null || sel >= state.detectedGames.size()) return;
        DetectedGame game = state.detectedGames.get(sel);
        state.setupGameName = game.displayName();
        state.setupGameDir = game.installPath().toString();
        state.setupProfileId = null;
        state.setupOutputDir = AppPaths.reportsForGame(game.safeId()).toString();
        state.setupDeep = true;
        state.scanResult = null;
        state.scanError = null;
        state.scanStarted = false;
        state.scanLog.clear();
        state.screen = TuiScreen.SCAN_SETUP;
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();
        Block outer = Block.builder()
            .title(Title.from(" ModScope — Installed Games "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outer.inner(area);
        frame.renderWidget(outer, area);

        int y = inner.y();
        frame.renderWidget(
            Paragraph.builder().text(Text.from(Line.from(
                Span.styled("  READ-ONLY — ModScope inspects but never modifies game files.",
                    Style.EMPTY.fg(Color.GREEN))))).build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        List<DetectedGame> games = state.detectedGames;
        String status = games.isEmpty()
            ? "  Steam not found or no games installed."
            : "  " + games.size() + " game(s) detected"
              + (state.scanHistory.size() > 0 ? "  ·  " + state.scanHistory.size() + " triaged" : "")
              + (state.scanAllQuickActive
                  ? "  ·  triaging " + state.scanAllCurrentGame + " (" + state.scanAllCompleted + "/" + state.scanAllTotal + ")"
                  : "");
        frame.renderWidget(
            Paragraph.builder().text(Text.from(Line.from(
                Span.styled(status, Style.EMPTY.fg(Color.CYAN))))).build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        y++; // spacer

        // List occupies most of the area; leave 2 rows for status panel + spacing.
        int listHeight = inner.height() - (y - inner.y()) - 2;
        if (listHeight > 0) {
            String[] items = buildMenuItems(state);
            Rect listArea = Rect.of(new Position(inner.x(), y), new Size(inner.width(), listHeight));
            frame.renderStatefulWidget(
                ListWidget.builder()
                    .items(items)
                    .highlightStyle(Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD))
                    .highlightSymbol("▶ ")
                    .build(),
                listArea,
                state.homeList
            );
        }

        StatusPanel.render(frame, inner, "Enter details   t triage   T triage-all   d deep-scan   r library   s settings   q quit");
    }

    static String[] buildMenuItems(TuiState state) {
        List<DetectedGame> games = state.detectedGames;
        String[] items = new String[games.size() + TAIL_ITEMS];
        int nameWidth = 36;
        for (int i = 0; i < games.size(); i++) {
            DetectedGame g = games.get(i);
            GameTriageResult triage = state.triageCache.get(g.id());
            Optional<ScanHistoryEntry> hist = state.scanHistory.find(g.id());
            String name = truncate(g.displayName(), nameWidth);
            String engine;
            String surface;
            String next;
            String badges;
            if (triage != null) {
                engine = String.valueOf(triage.detectedEngine());
                surface = triage.looseFileSurface().name();
                next = triage.recommendedAction().name();
                badges = BadgeBar.inline(triage.badges());
            } else if (hist.isPresent()) {
                engine = orDash(hist.get().engineHint());
                surface = orDash(hist.get().surfaceScore());
                next = orDash(hist.get().recommendedAction());
                badges = "";
            } else {
                engine = "—";
                surface = "—";
                next = "press t to triage";
                badges = "";
            }
            items[i] = String.format("  %-" + nameWidth + "s  %-10s  %-8s  %-22s  %s",
                name, truncate(engine, 10), truncate(surface, 8), truncate(next, 22), badges);
        }
        int base = games.size();
        items[base]     = games.isEmpty() ? "  [ Scan game folder manually ]" : "  [ Scan all installed games quickly ]";
        items[base + 1] = "  [ Open library summary ]";
        items[base + 2] = "  [ Scan a different folder ]";
        items[base + 3] = "  [ Exit ]";
        return items;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
