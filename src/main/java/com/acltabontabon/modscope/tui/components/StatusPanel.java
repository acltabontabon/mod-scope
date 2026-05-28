package com.acltabontabon.modscope.tui.components;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.paragraph.Paragraph;

public final class StatusPanel {

    private StatusPanel() {}

    public static void render(Frame frame, Rect area, String hint) {
        Rect barArea = Rect.of(
            new Position(area.x(), area.y() + area.height() - 1),
            new Size(area.width(), 1)
        );
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from("  " + hint))
                .style(Style.EMPTY.bg(Color.DARK_GRAY).fg(Color.WHITE))
                .build(),
            barArea
        );
    }
}
