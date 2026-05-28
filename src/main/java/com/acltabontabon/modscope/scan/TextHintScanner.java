package com.acltabontabon.modscope.scan;

import com.acltabontabon.modscope.util.SafeIo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TextHintScanner {

    private static final long MAX_SCAN_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final int SNIPPET_RADIUS = 40;

    private TextHintScanner() {}

    public static List<HintMatch> scan(Path file, String extension) {
        long size = SafeIo.fileSize(file);
        if (size <= 0 || size > MAX_SCAN_SIZE) return List.of();
        if (!FileClassifier.isTextReadable(extension)) return List.of();

        List<String> lines = SafeIo.readLines(file);
        List<HintMatch> matches = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String lower = lines.get(i).toLowerCase();
            for (String keyword : HintKeywordSet.KEYWORDS) {
                int idx = lower.indexOf(keyword);
                if (idx >= 0) {
                    String snippet = buildSnippet(lines.get(i), idx, keyword.length());
                    HintMatch.Confidence confidence = confidenceFor(extension);
                    matches.add(new HintMatch(file.toString(), i + 1, keyword, snippet, confidence));
                    break; // one match per keyword per line
                }
            }
        }
        return matches;
    }

    private static String buildSnippet(String line, int matchIdx, int keywordLen) {
        int start = Math.max(0, matchIdx - SNIPPET_RADIUS);
        int end = Math.min(line.length(), matchIdx + keywordLen + SNIPPET_RADIUS);
        String snippet = line.substring(start, end).strip();
        if (snippet.length() > 120) snippet = snippet.substring(0, 117) + "...";
        return snippet;
    }

    private static HintMatch.Confidence confidenceFor(String extension) {
        return switch (extension.toLowerCase()) {
            case "cfg", "ini", "json", "xml", "yaml", "yml", "config", "toml" ->
                HintMatch.Confidence.HIGH;
            case "txt", "log", "lua", "properties" ->
                HintMatch.Confidence.MEDIUM;
            default -> HintMatch.Confidence.LOW;
        };
    }
}
