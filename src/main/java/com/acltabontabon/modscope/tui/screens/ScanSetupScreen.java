package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.core.ScanOptions;
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
            case 2 -> state.setupDeep = !state.setupDeep; // toggle deep
            case 3 -> { // start scan
                state.screen = TuiScreen.SCAN_PROGRESS;
                ScanProgressScreen.startScan(state, runner);
            }
            case 4 -> state.screen = TuiScreen.HOME; // back
        }
        return true;
    }

    private static String[] buildItems(TuiState state) {
        String profile = state.setupProfileId != null ? state.setupProfileId : "auto-detect";
        String gameDir = state.setupGameDir.isBlank() ? "(auto via Steam)" : state.setupGameDir;
        return new String[]{
            "Profile:    " + profile,
            "Directory:  " + gameDir,
            "Deep scan:  " + (state.setupDeep ? "ON" : "OFF"),
            "[ Start Scan ]",
            "[ Back ]"
        };
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();

        Block outerBlock = Block.builder()
            .title(Title.from(" Scan Setup "))
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
        return new ScanOptions(state.setupProfileId, gameDir, outputDir, mode);
    }
}
