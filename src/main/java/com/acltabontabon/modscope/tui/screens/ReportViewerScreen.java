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
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.ArrayList;
import java.util.List;

/** Read-only scrollable markdown viewer (line prefix coloring only — no real rendering). */
public final class ReportViewerScreen {

    private ReportViewerScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        return switch (event) {
            case KeyEvent k when k.isCancel() -> {
                state.screen = state.selectedGame != null ? TuiScreen.GAME_DETAILS : TuiScreen.HOME;
                yield true;
            }
            case KeyEvent k when k.isDown() -> { state.reportViewerScroll++; yield true; }
            case KeyEvent k when k.isUp()   -> { state.reportViewerScroll = Math.max(0, state.reportViewerScroll - 1); yield true; }
            case KeyEvent k when k.isQuit() -> { runner.quit(); yield false; }
            default -> false;
        };
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();
        String title = state.reportViewerPath != null
            ? " " + state.reportViewerPath.getFileName().toString() + " "
            : " Report ";
        Block outer = Block.builder()
            .title(Title.from(title))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outer.inner(area);
        frame.renderWidget(outer, area);

        if (state.reportViewerLines == null || state.reportViewerLines.isEmpty()) {
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(Span.styled("  No report loaded.", Style.EMPTY.fg(Color.YELLOW)))))
                    .build(),
                Rect.of(new Position(inner.x(), inner.y()), new Size(inner.width(), 1))
            );
            StatusPanel.render(frame, inner, "Esc back   q quit");
            return;
        }

        int height = Math.max(1, inner.height() - 1);
        int start = Math.min(state.reportViewerScroll, Math.max(0, state.reportViewerLines.size() - height));
        int end = Math.min(state.reportViewerLines.size(), start + height);
        List<String> visible = state.reportViewerLines.subList(start, end);

        List<Line> lines = new ArrayList<>();
        for (String raw : visible) {
            lines.add(Line.from(Span.styled(raw, styleFor(raw))));
        }
        frame.renderWidget(
            Paragraph.builder().text(Text.from(lines.toArray(Line[]::new))).build(),
            Rect.of(new Position(inner.x(), inner.y()), new Size(inner.width(), height))
        );

        StatusPanel.render(frame, inner, "↑↓ scroll   Esc back   q quit");
    }

    private static Style styleFor(String line) {
        String t = line.stripLeading();
        if (t.startsWith("# ")) return Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD);
        if (t.startsWith("## ")) return Style.EMPTY.fg(Color.MAGENTA).addModifier(Modifier.BOLD);
        if (t.startsWith("### ")) return Style.EMPTY.fg(Color.YELLOW).addModifier(Modifier.BOLD);
        if (t.startsWith("> ")) return Style.EMPTY.fg(Color.GREEN);
        if (t.startsWith("- ") || t.startsWith("* ")) return Style.EMPTY.fg(Color.WHITE);
        if (t.startsWith("---")) return Style.EMPTY.fg(Color.DARK_GRAY);
        return Style.EMPTY.fg(Color.WHITE);
    }
}
