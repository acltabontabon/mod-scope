package com.acltabontabon.modscope.tui.components;

import com.acltabontabon.modscope.triage.Badge;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.ArrayList;
import java.util.List;

public final class BadgeBar {

    private BadgeBar() {}

    public static void render(Frame frame, Rect area, List<Badge> badges) {
        if (badges == null || badges.isEmpty() || area.height() <= 0) return;
        List<Span> spans = new ArrayList<>();
        spans.add(Span.styled("  ", Style.EMPTY));
        for (Badge b : badges) {
            spans.add(Span.styled(" " + b.label() + " ", styleFor(b)));
            spans.add(Span.styled(" ", Style.EMPTY));
        }
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(spans.toArray(Span[]::new))))
                .build(),
            area
        );
    }

    public static String inline(List<Badge> badges) {
        if (badges == null || badges.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Badge b : badges) {
            sb.append('[').append(shortLabel(b)).append(']').append(' ');
        }
        return sb.toString();
    }

    private static String shortLabel(Badge b) {
        return switch (b) {
            case LOOSE_FILES -> "loose";
            case ARCHIVE_HEAVY -> "archives";
            case CONFIGS_FOUND -> "config";
            case SAVES_FOUND -> "save";
            case EXTERNAL_TOOL_NEEDED -> "ext-tool";
            case UNKNOWN_ENGINE -> "?engine";
            case HIGH_CONFIDENCE -> "hi-conf";
            case LOW_CONFIDENCE -> "lo-conf";
            case GOOD_FIRST_TARGET -> "good-first";
            case HARD_TARGET -> "hard";
        };
    }

    private static Style styleFor(Badge b) {
        return switch (b) {
            case LOOSE_FILES -> bg(Color.CYAN, Color.BLACK);
            case ARCHIVE_HEAVY -> bg(Color.YELLOW, Color.BLACK);
            case CONFIGS_FOUND -> bg(Color.GREEN, Color.BLACK);
            case SAVES_FOUND -> bg(Color.BLUE, Color.WHITE);
            case EXTERNAL_TOOL_NEEDED -> bg(Color.RED, Color.WHITE);
            case UNKNOWN_ENGINE -> bg(Color.DARK_GRAY, Color.WHITE);
            case HIGH_CONFIDENCE -> bg(Color.GREEN, Color.BLACK).addModifier(Modifier.BOLD);
            case LOW_CONFIDENCE -> bg(Color.YELLOW, Color.BLACK);
            case GOOD_FIRST_TARGET -> bg(Color.MAGENTA, Color.WHITE).addModifier(Modifier.BOLD);
            case HARD_TARGET -> bg(Color.RED, Color.WHITE).addModifier(Modifier.BOLD);
        };
    }

    private static Style bg(Color bg, Color fg) {
        return Style.EMPTY.bg(bg).fg(fg);
    }
}
