package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.core.ModdingSurfaceScore;
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

import java.util.ArrayList;
import java.util.List;

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

        // Summary header
        String gameName = result.install().map(i -> i.profile().displayName()).orElse("not detected");
        String installPath = result.install().map(i -> i.installPath().toString()).orElse("—");
        long savesFound = result.saves().stream().filter(s -> s.exists()).count();
        String engineLabel = result.engineDetection().isKnown()
            ? result.engineDetection().primary() + " (" + result.engineDetection().confidence() + "%)"
            : "unknown";

        renderLine(frame, leftX, y++, inner.width(), "Game:    ", gameName, Color.CYAN);
        renderLine(frame, leftX, y++, inner.width(), "Path:    ", shorten(installPath, inner.width() - 12), Color.WHITE);
        renderLine(frame, leftX, y++, inner.width(), "Files:   ", String.valueOf(result.files().size()), Color.WHITE);
        renderLine(frame, leftX, y++, inner.width(), "Saves:   ", savesFound + " found", Color.WHITE);
        renderLine(frame, leftX, y++, inner.width(), "Engine:  ", engineLabel, Color.MAGENTA);
        renderLine(frame, leftX, y++, inner.width(), "Reports: ", result.reportDir().toAbsolutePath().toString(), Color.YELLOW);

        y++; // spacer

        int remainingHeight = inner.height() - (y - inner.y()) - 1;
        if (remainingHeight < 4) {
            StatusPanel.render(frame, inner, "Esc back   q quit");
            return;
        }

        // Split remaining area: top row = files+hints, bottom row = leads+score
        int topH = remainingHeight * 2 / 3;
        int bottomH = remainingHeight - topH;

        if (topH >= 3) {
            // Left panel: file categories
            Block catBlock = Block.builder()
                .title(Title.from(" Files by Category "))
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .build();
            Rect catArea = Rect.of(new Position(leftX, y), new Size(halfW, topH));
            frame.renderWidget(catBlock, catArea);
            FileCategoryTable.render(frame, catBlock.inner(catArea), result.files());

            // Right panel: text hints
            Block hintBlock = Block.builder()
                .title(Title.from(" QoL Text Hints "))
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .build();
            Rect hintArea = Rect.of(new Position(rightX, y), new Size(inner.width() - halfW - 1, topH));
            frame.renderWidget(hintBlock, hintArea);
            HintResultsPanel.render(frame, hintBlock.inner(hintArea), result.hints());
        }

        if (bottomH >= 3) {
            int botY = y + topH;

            // Left bottom: Investigation Leads
            Block leadsBlock = Block.builder()
                .title(Title.from(" Investigation Leads "))
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .build();
            Rect leadsArea = Rect.of(new Position(leftX, botY), new Size(halfW, bottomH));
            frame.renderWidget(leadsBlock, leadsArea);
            renderLeads(frame, leadsBlock.inner(leadsArea), result);

            // Right bottom: Modding Surface Score
            Block scoreBlock = Block.builder()
                .title(Title.from(" Modding Surface Score "))
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .build();
            Rect scoreArea = Rect.of(new Position(rightX, botY), new Size(inner.width() - halfW - 1, bottomH));
            frame.renderWidget(scoreBlock, scoreArea);
            renderScore(frame, scoreBlock.inner(scoreArea), result);
        }

        StatusPanel.render(frame, inner, "Esc back to home   q quit");
    }

    private static void renderLeads(Frame frame, Rect area, ScanResult result) {
        List<Line> lines = new ArrayList<>();

        // Engine
        String engLabel = result.engineDetection().isKnown()
            ? result.engineDetection().primary() + " (" + result.engineDetection().confidence() + "%)"
            : "unknown";
        lines.add(Line.from(
            Span.styled("  Engine:    ", Style.EMPTY.fg(Color.GRAY)),
            Span.styled(engLabel, Style.EMPTY.fg(result.engineDetection().isKnown() ? Color.CYAN : Color.YELLOW))
        ));

        // Package definition
        if (result.packageDefinition().found()) {
            lines.add(Line.from(
                Span.styled("  PkgDef:    ", Style.EMPTY.fg(Color.GRAY)),
                Span.styled(result.packageDefinition().chunkCount() + " chunk(s) — see package-definition-analysis.md",
                    Style.EMPTY.fg(Color.GREEN))
            ));
        } else {
            lines.add(Line.from(
                Span.styled("  PkgDef:    ", Style.EMPTY.fg(Color.GRAY)),
                Span.styled("not found", Style.EMPTY.fg(Color.YELLOW))
            ));
        }

        // Binary hints — show useful vs. suppressed
        int binUseful = result.binaryScan().usefulCount();
        int binSuppressed = result.binaryScan().suppressedCount();
        int binTotal = result.binaryScan().allHints().size();
        String binLabel;
        Color binColor;
        if (binTotal == 0) {
            binLabel = "none";
            binColor = Color.YELLOW;
        } else if (binUseful == 0) {
            binLabel = "0 useful (" + binSuppressed + " vendor/runtime noise suppressed)";
            binColor = Color.YELLOW;
        } else {
            binLabel = binUseful + " useful / " + binTotal + " raw (" + binSuppressed + " suppressed)";
            binColor = Color.GREEN;
        }
        lines.add(Line.from(
            Span.styled("  BinHints:  ", Style.EMPTY.fg(Color.GRAY)),
            Span.styled(binLabel, Style.EMPTY.fg(binColor))
        ));

        // Save inventory
        int saveCount = result.saveInventory().size();
        lines.add(Line.from(
            Span.styled("  SaveFiles: ", Style.EMPTY.fg(Color.GRAY)),
            Span.styled(saveCount > 0 ? saveCount + " file(s) found" : "none",
                Style.EMPTY.fg(saveCount > 0 ? Color.WHITE : Color.YELLOW))
        ));

        renderLines(frame, area, lines);
    }

    private static void renderScore(Frame frame, Rect area, ScanResult result) {
        ModdingSurfaceScore score = result.surfaceScore();
        Color scoreColor = switch (score) {
            case NONE   -> Color.GRAY;
            case LOW    -> Color.YELLOW;
            case MEDIUM -> Color.CYAN;
            case HIGH   -> Color.GREEN;
        };
        String scoreDesc = switch (score) {
            case NONE   -> "Fully packed — no obvious mod entry point";
            case LOW    -> "Archive-heavy; known tools may apply";
            case MEDIUM -> "Known engine toolchain + some loose surface";
            case HIGH   -> "Clear mod surface: loose config, scripts, hints";
        };

        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(
            Span.styled("  " + score, Style.EMPTY.fg(scoreColor).addModifier(Modifier.BOLD))
        ));
        lines.add(Line.from(Span.styled("  " + scoreDesc, Style.EMPTY.fg(Color.GRAY))));

        // Show top engine signals inline
        if (!result.engineDetection().signals().isEmpty()) {
            lines.add(Line.from(Span.styled("", Style.EMPTY)));
            lines.add(Line.from(Span.styled("  Signals:", Style.EMPTY.fg(Color.GRAY))));
            result.engineDetection().signals().stream().limit(3).forEach(sig ->
                lines.add(Line.from(
                    Span.styled("    • " + sig.evidence(), Style.EMPTY.fg(Color.WHITE))
                ))
            );
        }

        renderLines(frame, area, lines);
    }

    private static void renderLines(Frame frame, Rect area, List<Line> lines) {
        if (lines.isEmpty() || area.height() <= 0) return;
        List<Line> visible = lines.size() > area.height()
            ? lines.subList(0, area.height())
            : lines;
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(visible.toArray(Line[]::new)))
                .build(),
            area
        );
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
