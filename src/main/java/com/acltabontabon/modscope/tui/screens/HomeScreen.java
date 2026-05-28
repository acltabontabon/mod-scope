package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.game.GameInstall;
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

    private HomeScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        String[] items = buildMenuItems(state);
        return switch (event) {
            case KeyEvent k when k.isDown() -> {
                state.homeList.selectNext(items.length);
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
        List<GameInstall> games = state.detectedGames;
        if (index < games.size()) {
            // Selected a detected game
            state.setupProfileId = games.get(index).profile().id();
            state.setupGameDir = games.get(index).installPath().toString();
            state.screen = TuiScreen.SCAN_SETUP;
        } else {
            int tail = index - games.size();
            switch (tail) {
                case 0 -> { // Scan folder manually
                    state.setupProfileId = null;
                    state.setupGameDir = "";
                    state.screen = TuiScreen.SCAN_SETUP;
                }
                case 1 -> runner.quit(); // Exit
            }
        }
        return true;
    }

    static String[] buildMenuItems(TuiState state) {
        List<GameInstall> games = state.detectedGames;
        String[] items = new String[games.size() + 2];
        for (int i = 0; i < games.size(); i++) {
            GameInstall g = games.get(i);
            items[i] = "Scan " + g.profile().displayName();
        }
        int tail = games.size();
        items[tail]     = games.isEmpty() ? "Scan game folder manually" : "Scan a different folder";
        items[tail + 1] = "Exit";
        return items;
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

        // Tagline
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  Inspect game files before building mods.", Style.EMPTY.fg(Color.GRAY))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        // Safety note
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  READ-ONLY — ModScope will not modify game files.", Style.EMPTY.fg(Color.GREEN))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        // Detection status line
        String detectionLine = state.detectedGames.isEmpty()
            ? "  No supported Steam games detected."
            : "  " + state.detectedGames.size() + " supported game(s) detected via Steam:";
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled(detectionLine, Style.EMPTY.fg(Color.CYAN))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        y++; // spacer before menu

        int listHeight = inner.height() - (y - inner.y()) - 2;
        if (listHeight > 0) {
            Rect listArea = Rect.of(new Position(inner.x(), y), new Size(inner.width(), listHeight));
            frame.renderStatefulWidget(
                ListWidget.builder()
                    .items(buildMenuItems(state))
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
