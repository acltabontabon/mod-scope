package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.core.ScanOptions;
import com.acltabontabon.modscope.scan.BinaryScanPolicy;
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

import java.nio.file.Path;

public final class ScanSetupScreen {

    private ScanSetupScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        String[] items = buildItems(state);
        return switch (event) {
            case KeyEvent k when k.isDown() -> {
                state.setupList.selectNext(items.length);
                yield true;
            }
            case KeyEvent k when k.isUp() -> {
                state.setupList.selectPrevious();
                yield true;
            }
            case KeyEvent k when k.isSelect() -> {
                int sel = state.setupList.selected() != null ? state.setupList.selected() : 0;
                yield handleSelection(sel, runner, state);
            }
            case KeyEvent k when k.isCancel() -> {
                state.screen = TuiScreen.HOME;
                yield true;
            }
            default -> false;
        };
    }

    private static boolean handleSelection(int index, TuiRunner runner, TuiState state) {
        switch (index) {
            case 2 -> state.setupDeep = !state.setupDeep;
            case 3 -> state.setupIncludeGameExe = !state.setupIncludeGameExe;
            case 4 -> state.setupIncludeVendorLibs = !state.setupIncludeVendorLibs;
            case 5 -> state.setupIncludeLargeArchives = !state.setupIncludeLargeArchives;
            case 6 -> { // start scan
                state.screen = TuiScreen.SCAN_PROGRESS;
                ScanProgressScreen.startScan(state, runner);
            }
            case 7 -> state.screen = TuiScreen.HOME;
        }
        return true;
    }

    private static String[] buildItems(TuiState state) {
        String gameDisplay = state.setupGameDir.isBlank() ? "(choose folder)" : shorten(state.setupGameDir, 48);
        String profileDisplay = state.setupProfileId != null ? state.setupProfileId : "generic (no profile)";
        return new String[]{
            "Directory:                " + gameDisplay,
            "Profile:                  " + profileDisplay,
            "Deep scan:                " + (state.setupDeep ? "ON" : "OFF"),
            "Binary: game exe:         " + (state.setupIncludeGameExe ? "ON" : "OFF"),
            "Binary: vendor libs:      " + (state.setupIncludeVendorLibs ? "ON" : "OFF"),
            "Binary: large archives:   " + (state.setupIncludeLargeArchives ? "ON" : "OFF"),
            "[ Start Scan ]",
            "[ Back ]"
        };
    }

    private static String shorten(String s, int max) {
        if (s.length() <= max) return s;
        return "..." + s.substring(s.length() - Math.max(0, max - 3));
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();

        String titleText = state.setupGameName.isBlank()
            ? " Scan Setup "
            : " Scan Setup — " + state.setupGameName + " ";
        Block outerBlock = Block.builder()
            .title(Title.from(titleText))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outerBlock.inner(area);
        frame.renderWidget(outerBlock, area);

        // Safety warning
        Rect warnArea = Rect.of(new Position(inner.x(), inner.y()), new Size(inner.width(), 1));
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  ⚠  READ-ONLY SCAN — ModScope will not modify game files.", Style.EMPTY.fg(Color.YELLOW).addModifier(Modifier.BOLD))
                )))
                .build(),
            warnArea
        );

        // Form list
        int listTop = inner.y() + 2;
        int listHeight = inner.height() - 4;
        if (listHeight > 0) {
            Rect listArea = Rect.of(new Position(inner.x(), listTop), new Size(inner.width(), listHeight));
            String[] items = buildItems(state);
            frame.renderStatefulWidget(
                ListWidget.builder()
                    .items(items)
                    .highlightStyle(Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD))
                    .highlightSymbol("▶ ")
                    .build(),
                listArea,
                state.setupList
            );
        }

        StatusPanel.render(frame, inner, "↑↓ navigate   Enter select/toggle   Esc back");
    }

    public static ScanOptions buildOptions(TuiState state) {
        Path gameDir = state.setupGameDir.isBlank() ? null : Path.of(state.setupGameDir);
        Path outputDir = Path.of(state.setupOutputDir);
        ScanMode mode = state.setupDeep ? ScanMode.DEEP : ScanMode.STANDARD;
        BinaryScanPolicy binaryPolicy = new BinaryScanPolicy(
            state.setupIncludeGameExe,
            state.setupIncludeVendorLibs,
            state.setupIncludeLargeArchives
        );
        return new ScanOptions(state.setupProfileId, gameDir, outputDir, mode, binaryPolicy);
    }
}
