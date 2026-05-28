package com.acltabontabon.modscope.tui.components;

import com.acltabontabon.modscope.scan.HintMatch;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HintResultsPanel {

    private HintResultsPanel() {}

    public static void render(Frame frame, Rect area, List<HintMatch> hints) {
        if (hints.isEmpty()) {
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(Span.styled("  No hints found.", Style.EMPTY.fg(Color.GRAY)))))
                    .build(),
                area
            );
            return;
        }

        Map<String, Long> byKeyword = hints.stream()
            .collect(Collectors.groupingBy(HintMatch::keyword, Collectors.counting()));

        Line header = Line.from(
            Span.styled("  QoL Hints (" + hints.size() + " matches):", Style.EMPTY.addModifier(Modifier.BOLD).fg(Color.GREEN))
        );

        Line[] rows = new Line[Math.min(byKeyword.size() + 1, area.height())];
        rows[0] = header;
        int i = 1;
        for (var entry : byKeyword.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(area.height() - 1)
                .toList()) {
            rows[i++] = Line.from(
                Span.styled(String.format("  %-20s %3d", entry.getKey(), entry.getValue()),
                    Style.EMPTY.fg(Color.CYAN))
            );
        }

        int nonNull = 0;
        for (Line row : rows) if (row != null) nonNull++;
        Line[] trimmed = new Line[nonNull];
        int j = 0;
        for (Line row : rows) if (row != null) trimmed[j++] = row;

        frame.renderWidget(
            Paragraph.builder().text(Text.from(trimmed)).build(),
            area
        );
    }
}
