package com.acltabontabon.modscope.scan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BinaryStringScanner {

    // Skip files larger than 512 MB to avoid stalling on huge archives
    private static final long MAX_FILE_SIZE = 512L * 1024 * 1024;
    // Sample the first and last 16 MB of each file
    private static final long SAMPLE_WINDOW = 16L * 1024 * 1024;
    // Minimum string length to consider extractable
    private static final int MIN_STRING_LEN = 6;
    // Context chars around the match
    private static final int CONTEXT_RADIUS = 40;

    private BinaryStringScanner() {}

    public static List<BinaryStringHint> scan(Path gameDir, List<FileEntry> files) {
        List<BinaryStringHint> results = new ArrayList<>();
        for (FileEntry entry : files) {
            if (!FileClassifier.isBinaryScannable(entry.category())) continue;
            if (entry.sizeBytes() <= 0 || entry.sizeBytes() > MAX_FILE_SIZE) continue;

            Path file = resolveFile(gameDir, entry.relativePath());
            if (!Files.isRegularFile(file)) continue;

            List<BinaryStringHint> fileHints = scanFile(file, entry.relativePath(), entry.sizeBytes());
            results.addAll(fileHints);
        }
        return results;
    }

    private static List<BinaryStringHint> scanFile(Path file, String relativePath, long fileSize) {
        List<BinaryStringHint> hints = new ArrayList<>();
        try {
            long tailStart = Math.max(0, fileSize - SAMPLE_WINDOW);
            // Read head window
            hints.addAll(scanRegion(file, 0, SAMPLE_WINDOW, relativePath));
            // Read tail window (only if it doesn't overlap with head)
            if (tailStart > SAMPLE_WINDOW) {
                hints.addAll(scanRegion(file, tailStart, fileSize, relativePath));
            }
        } catch (IOException ignored) {}
        return hints;
    }

    private static List<BinaryStringHint> scanRegion(Path file, long start, long end, String relativePath)
            throws IOException {
        List<BinaryStringHint> hints = new ArrayList<>();
        long limit = end - start;
        if (limit <= 0) return hints;

        byte[] buf = new byte[(int) Math.min(limit, SAMPLE_WINDOW)];
        int read;
        try (InputStream in = Files.newInputStream(file)) {
            long skipped = in.skip(start);
            if (skipped < start) return hints;
            read = in.readNBytes(buf, 0, buf.length);
        }
        if (read <= 0) return hints;

        // Extract printable ASCII strings and search for keywords
        StringBuilder current = new StringBuilder();
        int stringStart = 0;

        for (int i = 0; i <= read; i++) {
            boolean printable = i < read && isPrintable(buf[i]);
            if (printable) {
                if (current.isEmpty()) stringStart = i;
                current.append((char) buf[i]);
            } else {
                if (current.length() >= MIN_STRING_LEN) {
                    String s = current.toString();
                    checkKeywords(s, start + stringStart, relativePath, hints);
                }
                current.setLength(0);
            }
        }

        return hints;
    }

    private static void checkKeywords(String s, long offset, String relativePath, List<BinaryStringHint> hints) {
        String lower = s.toLowerCase();
        for (String keyword : HintKeywordSet.KEYWORDS) {
            int idx = lower.indexOf(keyword);
            if (idx >= 0) {
                int ctxStart = Math.max(0, idx - CONTEXT_RADIUS);
                int ctxEnd = Math.min(s.length(), idx + keyword.length() + CONTEXT_RADIUS);
                String ctx = s.substring(ctxStart, ctxEnd);
                if (ctx.length() > 120) ctx = ctx.substring(0, 117) + "...";
                hints.add(new BinaryStringHint(relativePath, offset + idx, keyword, ctx));
            }
        }
    }

    private static boolean isPrintable(byte b) {
        int c = b & 0xFF;
        return c >= 32 && c < 127;
    }

    private static Path resolveFile(Path gameDir, String relativePath) {
        String sep = String.valueOf(gameDir.getFileSystem().getSeparator().charAt(0));
        return gameDir.resolve(relativePath.replace('/', sep.charAt(0)));
    }
}
