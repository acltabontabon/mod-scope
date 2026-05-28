package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.recommendation.Recommendation;
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

public final class RecommendationScreen {

    private RecommendationScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        return switch (event) {
            case KeyEvent k when k.isCancel() -> {
                state.screen = state.selectedGame != null ? TuiScreen.GAME_DETAILS : TuiScreen.HOME;
                yield true;
            }
            case KeyEvent k when k.isQuit() -> { runner.quit(); yield false; }
            default -> false;
        };
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();
        Block outer = Block.builder()
            .title(Title.from(" Recommendations "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outer.inner(area);
        frame.renderWidget(outer, area);

        int y = inner.y();
        List<Recommendation> recs = state.currentRecommendations;
        if (recs == null || recs.isEmpty()) {
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(Span.styled("  No recommendations yet — run a triage or deep scan first.",
                        Style.EMPTY.fg(Color.YELLOW)))))
                    .build(),
                Rect.of(new Position(inner.x(), y), new Size(inner.width(), 1))
            );
            StatusPanel.render(frame, inner, "Esc back   q quit");
            return;
        }

        List<Line> lines = new ArrayList<>();
        int n = 1;
        for (Recommendation r : recs) {
            lines.add(Line.from(
                Span.styled("  " + n++ + ". " + r.title(), Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD))
            ));
            lines.add(Line.from(
                Span.styled("     ", Style.EMPTY),
                Span.styled("[" + r.type().name() + "]  ", Style.EMPTY.fg(Color.MAGENTA)),
                Span.styled("confidence " + r.confidence() + "%  ", Style.EMPTY.fg(Color.GRAY)),
                Span.styled("risk " + r.riskLevel().name(), Style.EMPTY.fg(colorForRisk(r)))
            ));
            for (String paragraph : wrap("Reason: " + r.reason(), inner.width() - 6)) {
                lines.add(Line.from(Span.styled("     " + paragraph, Style.EMPTY.fg(Color.WHITE))));
            }
            for (String paragraph : wrap("Suggested action: " + r.suggestedAction(), inner.width() - 6)) {
                lines.add(Line.from(Span.styled("     " + paragraph, Style.EMPTY.fg(Color.GREEN))));
            }
            lines.add(Line.from(Span.styled("", Style.EMPTY)));
        }

        int maxLines = inner.height() - 1;
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);

        frame.renderWidget(
            Paragraph.builder().text(Text.from(lines.toArray(Line[]::new))).build(),
            Rect.of(new Position(inner.x(), y), new Size(inner.width(), Math.max(1, inner.height() - 1)))
        );

        StatusPanel.render(frame, inner, "Esc back   q quit");
    }

    private static Color colorForRisk(Recommendation r) {
        return switch (r.riskLevel()) {
            case LOW -> Color.GREEN;
            case MEDIUM -> Color.YELLOW;
            case HIGH -> Color.RED;
        };
    }

    private static List<String> wrap(String text, int width) {
        if (width <= 10) width = 10;
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            if (cur.length() + word.length() + 1 > width) {
                if (cur.length() > 0) out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
