package com.acltabontabon.modscope.tui.components;

import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
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
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class FileCategoryTable {

    private FileCategoryTable() {}

    public static void render(Frame frame, Rect area, List<FileEntry> files) {
        Map<FileCategory, Long> counts = files.stream()
            .collect(Collectors.groupingBy(FileEntry::category, Collectors.counting()));

        Line header = Line.from(
            Span.styled(String.format("  %-22s %6s", "Category", "Files"), Style.EMPTY.addModifier(Modifier.BOLD))
        );

        Line[] rows = new Line[FileCategory.values().length + 1];
        rows[0] = header;
        int i = 1;
        for (FileCategory cat : FileCategory.values()) {
            long count = counts.getOrDefault(cat, 0L);
            if (count == 0) continue;
            Color color = colorFor(cat);
            rows[i++] = Line.from(
                Span.styled(String.format("  %-22s %6d", cat.name(), count), Style.EMPTY.fg(color))
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

    private static Color colorFor(FileCategory cat) {
        return switch (cat) {
            case GAME_EXECUTABLE  -> Color.RED;
            case RUNTIME_LIBRARY  -> Color.RED;
            case NVIDIA_LIBRARY   -> Color.GRAY;
            case STEAM_LIBRARY    -> Color.GRAY;
            case ARCHIVE          -> Color.YELLOW;
            case PACKAGE_DEFINITION -> Color.CYAN;
            case CONFIG           -> Color.CYAN;
            case TEXT             -> Color.GREEN;
            case VIDEO            -> Color.MAGENTA;
            case LOCALIZATION     -> Color.BLUE;
            case SHADER_CACHE     -> Color.GRAY;
            case UNKNOWN_LARGE    -> Color.RED;
            case OTHER            -> Color.WHITE;
        };
    }
}
