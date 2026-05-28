package com.acltabontabon.modscope.tui.screens;

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

public final class HomeScreen {

    private HomeScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        return switch (event) {
            case KeyEvent k when k.isDown() -> {
                state.homeList.selectNext(TuiState.HOME_ITEMS.length);
                yield true;
            }
            case KeyEvent k when k.isUp() -> {
                state.homeList.selectPrevious();
                yield true;
            }
            case KeyEvent k when k.isSelect() -> {
                int selected = state.homeList.selected() != null ? state.homeList.selected() : 0;
                yield handleSelection(selected, runner, state);
            }
            case KeyEvent k when k.isQuit() -> {
                runner.quit();
                yield false;
            }
            default -> false;
        };
    }

    private static boolean handleSelection(int index, TuiRunner runner, TuiState state) {
        switch (index) {
            case 0 -> { // Scan detected Steam games
                state.setupProfileId = null;
                state.setupGameDir = "";
                state.screen = TuiScreen.SCAN_SETUP;
            }
            case 1 -> { // Scan 007 First Light
                state.setupProfileId = "007-first-light";
                state.setupGameDir = "";
                state.screen = TuiScreen.SCAN_SETUP;
            }
            case 2 -> { // Choose game folder manually
                state.setupProfileId = null;
                state.setupGameDir = "";
                state.screen = TuiScreen.SCAN_SETUP;
            }
            case 3 -> { // Open recent report — not yet implemented
                // no-op for MVP
            }
            case 4 -> runner.quit(); // Exit
        }
        return true;
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

        // Tagline
        Rect taglineArea = Rect.of(new Position(inner.x(), inner.y()), new Size(inner.width(), 1));
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  Inspect game files before building mods.", Style.EMPTY.fg(Color.GRAY))
                )))
                .build(),
            taglineArea
        );

        // Safety note
        Rect noteArea = Rect.of(new Position(inner.x(), inner.y() + 1), new Size(inner.width(), 1));
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  READ-ONLY — ModScope will not modify game files.", Style.EMPTY.fg(Color.GREEN))
                )))
                .build(),
            noteArea
        );

        // Menu list
        int listTop = inner.y() + 3;
        int listHeight = inner.height() - 5;
        if (listHeight > 0) {
            Rect listArea = Rect.of(new Position(inner.x(), listTop), new Size(inner.width(), listHeight));
            frame.renderStatefulWidget(
                ListWidget.builder()
                    .items(TuiState.HOME_ITEMS)
                    .highlightStyle(Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD))
                    .highlightSymbol("▶ ")
                    .build(),
                listArea,
                state.homeList
            );
        }

        StatusPanel.render(frame, inner, "↑↓ navigate   Enter select   q quit");
    }
}
