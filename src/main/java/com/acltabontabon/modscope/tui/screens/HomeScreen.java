package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.game.GameProfileRegistry;
import com.acltabontabon.modscope.steam.SteamAppManifest;
import com.acltabontabon.modscope.tui.TuiScreen;
import com.acltabontabon.modscope.tui.TuiState;
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

public final class HomeScreen {

    private static final int TAIL_ITEMS = 2; // "Scan folder manually" + "Exit"

    private HomeScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        int total = state.detectedManifests.size() + TAIL_ITEMS;
        return switch (event) {
            case KeyEvent k when k.isDown() -> {
                state.homeList.selectNext(total);
                yield true;
            }
            case KeyEvent k when k.isUp() -> {
                state.homeList.selectPrevious();
                yield true;
            }
            case KeyEvent k when k.isSelect() -> {
                int sel = state.homeList.selected() != null ? state.homeList.selected() : 0;
                yield handleSelection(sel, runner, state);
            }
            case KeyEvent k when k.isQuit() -> {
                runner.quit();
                yield false;
            }
            default -> false;
        };
    }

    private static boolean handleSelection(int index, TuiRunner runner, TuiState state) {
        List<SteamAppManifest> manifests = state.detectedManifests;
        if (index < manifests.size()) {
            SteamAppManifest manifest = manifests.get(index);
            state.setupGameName = manifest.name();
            state.setupGameDir = manifest.resolvedInstallPath().toString();
            state.setupProfileId = GameProfileRegistry.findByAppId(manifest.appId())
                .map(p -> p.id()).orElse(null);
            resetScanState(state);
            state.screen = TuiScreen.SCAN_SETUP;
        } else {
            int tail = index - manifests.size();
            if (tail == 0) { // Scan folder manually
                state.setupGameName = "";
                state.setupGameDir = "";
                state.setupProfileId = null;
                resetScanState(state);
                state.screen = TuiScreen.SCAN_SETUP;
            } else { // Exit
                runner.quit();
            }
        }
        return true;
    }

    private static void resetScanState(TuiState state) {
        state.scanStarted = false;
        state.scanResult = null;
        state.scanError = null;
        state.scanLog.clear();
        state.currentPhase = "Starting...";
        state.filesScanned = 0;
        state.configLike = 0;
        state.archives = 0;
        state.videos = 0;
        state.hintsFound = 0;
        state.binaryHintsTotal = 0;
        state.binaryHintsUseful = 0;
        state.binaryHintsSuppressed = 0;
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();

        Block outerBlock = Block.builder()
            .title(Title.from(" ModScope "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outerBlock.inner(area);
        frame.renderWidget(outerBlock, area);

        int y = inner.y();

        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  Inspect game files before building mods.", Style.EMPTY.fg(Color.GRAY))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  READ-ONLY — ModScope will not modify game files.", Style.EMPTY.fg(Color.GREEN))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        y++; // spacer

        List<SteamAppManifest> manifests = state.detectedManifests;
        if (manifests.isEmpty()) {
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(
                        Span.styled("  Steam not found or no games installed.", Style.EMPTY.fg(Color.YELLOW))
                    )))
                    .build(),
                Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
            );
        } else {
            long knownCount = manifests.stream()
                .filter(m -> GameProfileRegistry.findByAppId(m.appId()).isPresent())
                .count();
            String statusLine = "  " + manifests.size() + " game(s) installed";
            if (knownCount > 0) {
                statusLine += "  ·  " + knownCount + " with full profile support";
            }
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(
                        Span.styled(statusLine, Style.EMPTY.fg(Color.CYAN))
                    )))
                    .build(),
                Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
            );
        }

        y++; // spacer before list

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

        StatusPanel.render(frame, inner, "↑↓ navigate   Enter select   q quit");
    }

    static String[] buildMenuItems(TuiState state) {
        List<SteamAppManifest> manifests = state.detectedManifests;
        String[] items = new String[manifests.size() + TAIL_ITEMS];
        for (int i = 0; i < manifests.size(); i++) {
            SteamAppManifest m = manifests.get(i);
            boolean known = GameProfileRegistry.findByAppId(m.appId()).isPresent();
            items[i] = known
                ? String.format("  %-48s [+]", m.name())
                : "  " + m.name();
        }
        int tail = manifests.size();
        items[tail]     = manifests.isEmpty() ? "  Scan game folder manually" : "  Scan a different folder";
        items[tail + 1] = "  Exit";
        return items;
    }
}
