package com.acltabontabon.modscope.tui.components;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.List;

public final class KeyValueList {

    private KeyValueList() {}

    public record Row(String key, String value, Color valueColor) {
        public static Row of(String k, String v) { return new Row(k, v, Color.WHITE); }
        public static Row of(String k, String v, Color c) { return new Row(k, v, c); }
    }

    public static void render(Frame frame, Rect area, List<Row> rows) {
        if (area.height() <= 0) return;
        int y = area.y();
        int maxKey = rows.stream().mapToInt(r -> r.key().length()).max().orElse(8);
        int colWidth = Math.min(maxKey + 2, Math.max(8, area.width() / 3));
        for (Row r : rows) {
            if (y >= area.y() + area.height()) break;
            String key = padRight(r.key(), colWidth);
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(
                        Span.styled("  " + key, Style.EMPTY.fg(Color.GRAY)),
                        Span.styled(r.value() == null ? "—" : r.value(),
                            Style.EMPTY.fg(r.valueColor() == null ? Color.WHITE : r.valueColor()))
                    )))
                    .build(),
                Rect.of(new Position(area.x(), y), new Size(area.width(), 1))
            );
            y++;
        }
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s;
        StringBuilder b = new StringBuilder(s);
        while (b.length() < n) b.append(' ');
        return b.toString();
    }
}
