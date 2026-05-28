package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.HintMatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class TextHintsMarkdownWriter {

    private TextHintsMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# QoL Text Hint Matches\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");

        if (result.hints().isEmpty()) {
            sb.append("No QoL hints found.\n");
        } else {
            sb.append("| File | Line | Keyword | Confidence | Snippet |\n");
            sb.append("|------|------|---------|------------|--------|\n");

            List<HintMatch> sorted = result.hints().stream()
                .sorted(Comparator.comparing(HintMatch::confidence)
                    .thenComparing(HintMatch::keyword))
                .toList();

            for (HintMatch match : sorted) {
                String file = truncate(match.filePath(), 60);
                String snippet = match.snippet().replace("|", "\\|");
                sb.append("| `").append(file).append("` | ")
                  .append(match.lineNumber()).append(" | ")
                  .append(match.keyword()).append(" | ")
                  .append(match.confidence()).append(" | ")
                  .append(snippet).append(" |\n");
            }
        }

        sb.append('\n');
        sb.append("---\n");
        sb.append("*These hints are read-only observations. ModScope did not modify any files.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return "..." + s.substring(s.length() - (max - 3));
    }
}
