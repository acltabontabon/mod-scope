package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.tui.TuiScreen;
import com.acltabontabon.modscope.tui.TuiState;
import com.acltabontabon.modscope.tui.components.FileCategoryTable;
import com.acltabontabon.modscope.tui.components.HintResultsPanel;
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

public final class ScanResultsScreen {

    private ScanResultsScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        return switch (event) {
            case KeyEvent k when k.isCancel() -> {
                state.screen = TuiScreen.HOME;
                yield true;
            }
            case KeyEvent k when k.isQuit() -> {
                runner.quit();
                yield false;
            }
            default -> false;
        };
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();
        ScanResult result = state.scanResult;
        if (result == null) return;

        Block outerBlock = Block.builder()
            .title(Title.from(" Scan Results "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outerBlock.inner(area);
        frame.renderWidget(outerBlock, area);

        int halfW = inner.width() / 2;
        int leftX = inner.x();
        int rightX = inner.x() + halfW + 1;
        int y = inner.y();

        // Summary
        String gameName = result.install().map(i -> i.profile().displayName()).orElse("not detected");
        String installPath = result.install().map(i -> i.installPath().toString()).orElse("—");
        long savesFound = result.saves().stream().filter(s -> s.exists()).count();

        renderLine(frame, leftX, y++, inner.width(), "Game:    ", gameName, Color.CYAN);
        renderLine(frame, leftX, y++, inner.width(), "Path:    ", shorten(installPath, inner.width() - 12), Color.WHITE);
        renderLine(frame, leftX, y++, inner.width(), "Files:   ", String.valueOf(result.files().size()), Color.WHITE);
        renderLine(frame, leftX, y++, inner.width(), "Saves:   ", savesFound + " found", Color.WHITE);
        renderLine(frame, leftX, y++, inner.width(), "Hints:   ", String.valueOf(result.hints().size()), Color.GREEN);
        renderLine(frame, leftX, y++, inner.width(), "Reports: ", result.reportDir().toAbsolutePath().toString(), Color.YELLOW);

        y++; // spacer

        // Left panel: file categories
        int tableHeight = Math.max(2, (inner.height() - y + inner.y()) / 2 - 1);
        if (y + tableHeight < inner.y() + inner.height() - 2) {
            Block catBlock = Block.builder()
                .title(Title.from(" Files by Category "))
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .build();
            Rect catArea = Rect.of(new Position(leftX, y), new Size(halfW, tableHeight));
            Rect catInner = catBlock.inner(catArea);
            frame.renderWidget(catBlock, catArea);
            FileCategoryTable.render(frame, catInner, result.files());

            // Right panel: hints
            Block hintBlock = Block.builder()
                .title(Title.from(" QoL Hints "))
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .build();
            Rect hintArea = Rect.of(new Position(rightX, y), new Size(inner.width() - halfW - 1, tableHeight));
            Rect hintInner = hintBlock.inner(hintArea);
            frame.renderWidget(hintBlock, hintArea);
            HintResultsPanel.render(frame, hintInner, result.hints());
        }

        StatusPanel.render(frame, inner, "Esc back to home   q quit");
    }

    private static void renderLine(Frame frame, int x, int y, int width, String label, String value, Color valueColor) {
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  " + label, Style.EMPTY.fg(Color.GRAY)),
                    Span.styled(value, Style.EMPTY.fg(valueColor))
                )))
                .build(),
            Rect.of(new Position(x, y), new Size(width, 1))
        );
    }

    private static String shorten(String s, int max) {
        if (s.length() <= max) return s;
        return "..." + s.substring(s.length() - Math.max(0, max - 3));
    }
}
