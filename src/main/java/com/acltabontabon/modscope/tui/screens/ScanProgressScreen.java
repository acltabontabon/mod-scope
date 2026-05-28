package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.core.ScanOptions;
import com.acltabontabon.modscope.core.ScanProgressListener;
import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.tui.TuiScreen;
import com.acltabontabon.modscope.tui.TuiState;
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
import java.util.List;

public final class ScanProgressScreen {

    private ScanProgressScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        if (state.scanResult != null || state.scanError != null) {
            return switch (event) {
                case KeyEvent k when k.isSelect() || k.isCancel() -> {
                    state.screen = state.scanError != null ? TuiScreen.HOME : TuiScreen.SCAN_RESULTS;
                    yield true;
                }
                case KeyEvent k when k.isQuit() -> {
                    runner.quit();
                    yield false;
                }
                default -> false;
            };
        }
        return false;
    }

    public static void startScan(TuiState state, TuiRunner runner) {
        if (state.scanStarted) return;
        state.scanStarted = true;
        state.currentPhase = "Starting...";

        ScanOptions options = ScanSetupScreen.buildOptions(state);

        Thread.ofVirtual().start(() -> {
            try {
                ScanResult result = state.scanService.scan(options, new ScanProgressListener() {
                    @Override
                    public void onPhaseStarted(String phase) {
                        runner.runOnRenderThread(() -> {
                            state.currentPhase = phase;
                            state.scanLog.add("[>] " + phase);
                        });
                    }

                    @Override
                    public void onProgress(int filesScanned, int configLike, int archives, int videos, int hints) {
                        runner.runOnRenderThread(() -> {
                            state.filesScanned = filesScanned;
                            state.configLike = configLike;
                            state.archives = archives;
                            state.videos = videos;
                            state.hintsFound = hints;
                        });
                    }

                    @Override
                    public void onLog(String message) {
                        runner.runOnRenderThread(() -> state.scanLog.add(message));
                    }

                    @Override
                    public void onComplete(ScanResult r) {}

                    @Override
                    public void onError(Exception error) {}
                });
                runner.runOnRenderThread(() -> {
                    state.scanResult = result;
                    state.hintsFound = result.hints().size();
                    state.filesScanned = result.files().size();
                    state.scanLog.add("Scan complete!");
                });
            } catch (Exception e) {
                runner.runOnRenderThread(() -> {
                    state.scanError = e;
                    state.scanLog.add("! Error: " + e.getMessage());
                });
            }
        });
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();

        Block outerBlock = Block.builder()
            .title(Title.from(" Scanning... "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outerBlock.inner(area);
        frame.renderWidget(outerBlock, area);

        int y = inner.y();

        // Phase line
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled("  Phase: ", Style.EMPTY.fg(Color.GRAY)),
                    Span.styled(state.currentPhase, Style.EMPTY.fg(Color.WHITE).addModifier(Modifier.BOLD))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y++), new Size(inner.width(), 1))
        );

        y++; // spacer

        // Counters
        renderCounter(frame, inner, y++, "Files scanned", state.filesScanned, Color.WHITE);
        renderCounter(frame, inner, y++, "Config-like",   state.configLike,   Color.CYAN);
        renderCounter(frame, inner, y++, "Archives",      state.archives,     Color.YELLOW);
        renderCounter(frame, inner, y++, "Videos",        state.videos,       Color.MAGENTA);
        renderCounter(frame, inner, y++, "Hints found",   state.hintsFound,   Color.GREEN);

        y++; // spacer before log

        // Log panel
        int logTop = y;
        int logHeight = inner.height() - (logTop - inner.y()) - 1;
        if (logHeight >= 2) {
            Rect logArea = Rect.of(new Position(inner.x(), logTop), new Size(inner.width(), logHeight));
            renderLog(frame, logArea, state.scanLog, state.scanError != null, state.scanResult != null);
        }

        // Done/error footer
        if (state.scanError != null || state.scanResult != null) {
            int footerY = inner.y() + inner.height() - 1;
            String footerMsg = state.scanError != null
                ? "Press Enter to return to home"
                : "Press Enter to view results";
            Color footerColor = state.scanError != null ? Color.RED : Color.GREEN;
            frame.renderWidget(
                Paragraph.builder()
                    .text(Text.from(Line.from(
                        Span.styled("  " + footerMsg, Style.EMPTY.fg(footerColor).addModifier(Modifier.BOLD))
                    )))
                    .build(),
                Rect.of(new Position(inner.x(), footerY), new Size(inner.width(), 1))
            );
        }
    }

    private static void renderLog(Frame frame, Rect area, List<String> log, boolean hasError, boolean isDone) {
        Block logBlock = Block.builder()
            .title(Title.from(" Log "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect logInner = logBlock.inner(area);
        frame.renderWidget(logBlock, area);

        if (log.isEmpty() || logInner.height() <= 0) return;

        // Show only the lines that fit, newest at the bottom
        int visible = logInner.height();
        List<String> recent = log.size() > visible
            ? log.subList(log.size() - visible, log.size())
            : log;

        Line[] lines = new Line[recent.size()];
        for (int i = 0; i < recent.size(); i++) {
            lines[i] = Line.from(Span.styled("  " + recent.get(i), styleFor(recent.get(i))));
        }

        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(lines))
                .build(),
            logInner
        );
    }

    private static Style styleFor(String msg) {
        if (msg.startsWith("[>]"))  return Style.EMPTY.fg(Color.WHITE).addModifier(Modifier.BOLD);
        if (msg.startsWith("+"))    return Style.EMPTY.fg(Color.GREEN);
        if (msg.startsWith("*"))    return Style.EMPTY.fg(Color.CYAN);
        if (msg.startsWith("!"))    return Style.EMPTY.fg(Color.RED);
        if (msg.startsWith("~"))    return Style.EMPTY.fg(Color.YELLOW);
        if (msg.startsWith("  "))   return Style.EMPTY.fg(Color.GRAY);
        return Style.EMPTY.fg(Color.WHITE);
    }

    private static void renderCounter(Frame frame, Rect inner, int y, String label, int value, Color color) {
        if (y >= inner.y() + inner.height() - 1) return;
        frame.renderWidget(
            Paragraph.builder()
                .text(Text.from(Line.from(
                    Span.styled(String.format("  %-20s", label), Style.EMPTY.fg(Color.GRAY)),
                    Span.styled(String.valueOf(value), Style.EMPTY.fg(color).addModifier(Modifier.BOLD))
                )))
                .build(),
            Rect.of(new Position(inner.x(), y), new Size(inner.width(), 1))
        );
    }
}
