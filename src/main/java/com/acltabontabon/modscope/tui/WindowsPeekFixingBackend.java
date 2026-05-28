package com.acltabontabon.modscope.tui;

import dev.tamboui.backend.panama.PanamaBackend;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.AbstractBackend;

import java.io.IOException;

/**
 * WindowsTerminal.peek() returns 0 when events are available rather than the actual
 * character value. EventParser.parseEscapeSequence() checks peek() == '[' to detect
 * CSI sequences; without this fix, arrow keys (ESC [A/B/C/D) are never parsed correctly
 * and arrive as UNKNOWN events, making navigation impossible on Windows.
 *
 * This wrapper buffers one character so peek() returns the real value.
 */
final class WindowsPeekFixingBackend extends AbstractBackend {

    private final PanamaBackend delegate;
    private int peeked = -2;

    WindowsPeekFixingBackend(PanamaBackend delegate) {
        this.delegate = delegate;
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        if (peeked != -2) return peeked;
        peeked = delegate.read(timeoutMs);
        return peeked;
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        if (peeked != -2) {
            int c = peeked;
            peeked = -2;
            return c;
        }
        return delegate.read(timeoutMs);
    }

    @Override public void flush()                                  throws IOException { delegate.flush(); }
    @Override public void clear()                                  throws IOException { delegate.clear(); }
    @Override public Size size()                                   throws IOException { return delegate.size(); }
    @Override public void showCursor()                             throws IOException { delegate.showCursor(); }
    @Override public void hideCursor()                             throws IOException { delegate.hideCursor(); }
    @Override public Position getCursorPosition()                  throws IOException { return delegate.getCursorPosition(); }
    @Override public void enterAlternateScreen()                   throws IOException { delegate.enterAlternateScreen(); }
    @Override public void leaveAlternateScreen()                   throws IOException { delegate.leaveAlternateScreen(); }
    @Override public void enableRawMode()                          throws IOException { delegate.enableRawMode(); }
    @Override public void disableRawMode()                         throws IOException { delegate.disableRawMode(); }
    @Override public void enableMouseCapture()                     throws IOException { delegate.enableMouseCapture(); }
    @Override public void disableMouseCapture()                    throws IOException { delegate.disableMouseCapture(); }
    @Override public void scrollUp(int lines)                      throws IOException { delegate.scrollUp(lines); }
    @Override public void scrollDown(int lines)                    throws IOException { delegate.scrollDown(lines); }
    @Override public void onResize(Runnable handler)                                  { delegate.onResize(handler); }
    @Override public void writeRaw(byte[] data)                    throws IOException { delegate.writeRaw(data); }
    @Override public void writeRaw(String data)                    throws IOException { delegate.writeRaw(data); }
    @Override public void close()                                  throws IOException { delegate.close(); }
}
