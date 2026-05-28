package com.acltabontabon.modscope.scan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BinaryStringScanner {

    // Hard ceiling regardless of policy (avoid stalling on massive files)
    static final long MAX_FILE_SIZE = 512L * 1024 * 1024;
    // Sample window per region (head + tail)
    private static final long SAMPLE_WINDOW = 16L * 1024 * 1024;
    // Minimum printable-ASCII run to treat as a string
    private static final int MIN_STRING_LEN = 6;
    // Context chars either side of the matched keyword
    private static final int CONTEXT_RADIUS = 40;

    private BinaryStringScanner() {}

    public static BinaryScanResult scan(Path gameDir, List<FileEntry> files, BinaryScanPolicy policy) {
        List<BinaryStringHint> allHints = new ArrayList<>();
        int scanned = 0;
        int skipped = 0;

        for (FileEntry entry : files) {
            if (!policy.shouldScan(entry.category(), entry.sizeBytes())) {
                skipped++;
                continue;
            }
            if (entry.sizeBytes() <= 0 || entry.sizeBytes() > MAX_FILE_SIZE) {
                skipped++;
                continue;
            }

            Path file = resolveFile(gameDir, entry.relativePath());
            if (!Files.isRegularFile(file)) {
                skipped++;
                continue;
            }

            List<BinaryStringHint> fileHints = scanFile(file, entry.relativePath(),
                entry.category(), entry.sizeBytes());
            allHints.addAll(fileHints);
            scanned++;
        }

        return new BinaryScanResult(List.copyOf(allHints), scanned, skipped, policy);
    }

    private static List<BinaryStringHint> scanFile(
            Path file, String relativePath, FileCategory category, long fileSize) {
        List<BinaryStringHint> hints = new ArrayList<>();
        try {
            hints.addAll(scanRegion(file, 0, Math.min(fileSize, SAMPLE_WINDOW), relativePath, category));
            long tailStart = fileSize - SAMPLE_WINDOW;
            if (tailStart > SAMPLE_WINDOW) {
                hints.addAll(scanRegion(file, tailStart, fileSize, relativePath, category));
            }
        } catch (IOException ignored) {}
        return hints;
    }

    private static List<BinaryStringHint> scanRegion(
            Path file, long start, long end, String relativePath, FileCategory category)
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

        // Extract printable ASCII runs and search each one for QoL keywords
        StringBuilder current = new StringBuilder();
        int stringStart = 0;
        for (int i = 0; i <= read; i++) {
            boolean printable = i < read && isPrintable(buf[i]);
            if (printable) {
                if (current.isEmpty()) stringStart = i;
                current.append((char) buf[i]);
            } else {
                if (current.length() >= MIN_STRING_LEN) {
                    checkKeywords(current.toString(), start + stringStart, relativePath, category, hints);
                }
                current.setLength(0);
            }
        }
        return hints;
    }

    private static void checkKeywords(
            String s, long offset, String relativePath, FileCategory category,
            List<BinaryStringHint> hints) {
        String lower = s.toLowerCase();
        for (String keyword : HintKeywordSet.KEYWORDS) {
            int idx = lower.indexOf(keyword);
            if (idx < 0) continue;

            int ctxStart = Math.max(0, idx - CONTEXT_RADIUS);
            int ctxEnd = Math.min(s.length(), idx + keyword.length() + CONTEXT_RADIUS);
            String ctx = s.substring(ctxStart, ctxEnd);
            if (ctx.length() > 120) ctx = ctx.substring(0, 117) + "...";

            BinaryHintRelevance relevance = BinaryHintScorer.score(category, ctx);
            String suppressionReason = relevance == BinaryHintRelevance.NOISE
                ? BinaryHintScorer.suppressionReason(category, ctx) : null;
            String explanation = (relevance == BinaryHintRelevance.HIGH || relevance == BinaryHintRelevance.MEDIUM)
                ? BinaryHintScorer.confidenceExplanation(category, ctx) : null;

            hints.add(new BinaryStringHint(
                relativePath, category, offset + idx,
                keyword, ctx, relevance, suppressionReason, explanation));
        }
    }

    private static boolean isPrintable(byte b) {
        int c = b & 0xFF;
        return c >= 32 && c < 127;
    }

    private static Path resolveFile(Path gameDir, String relativePath) {
        char sep = gameDir.getFileSystem().getSeparator().charAt(0);
        return gameDir.resolve(relativePath.replace('/', sep));
    }
}
