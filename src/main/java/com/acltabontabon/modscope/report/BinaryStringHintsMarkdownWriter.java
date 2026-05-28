package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.BinaryHintRelevance;
import com.acltabontabon.modscope.scan.BinaryScanResult;
import com.acltabontabon.modscope.scan.BinaryStringHint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BinaryStringHintsMarkdownWriter {

    private BinaryStringHintsMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        BinaryScanResult scan = result.binaryScan();
        StringBuilder sb = new StringBuilder();
        sb.append("# Binary String Hints\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");

        // Summary section
        sb.append("## Summary\n\n");
        sb.append("| Metric | Value |\n|--------|-------|\n");
        sb.append("| Total raw hits | ").append(scan.allHints().size()).append(" |\n");
        sb.append("| Useful hits | **").append(scan.usefulCount()).append("** ");
        if (scan.usefulCount() > 0) {
            sb.append("(HIGH: ").append(scan.highHints().size())
              .append(", MEDIUM: ").append(scan.mediumHints().size())
              .append(", LOW: ").append(scan.lowHints().size()).append(")");
        }
        sb.append(" |\n");
        sb.append("| Suppressed/noise | ").append(scan.suppressedCount()).append(" |\n");
        sb.append("| Files scanned | ").append(scan.filesScanned()).append(" |\n");
        sb.append("| Files skipped | ").append(scan.filesSkipped()).append(" |\n");
        sb.append("| Scan policy | ").append(scan.policy().description()).append(" |\n");
        sb.append('\n');

        if (scan.allHints().isEmpty()) {
            sb.append("No binary string hints found under current scan policy.\n");
            writeFile(outputFile, sb);
            return;
        }

        if (scan.usefulCount() == 0) {
            sb.append("**No high-confidence game-specific QoL strings were found.** ");
            sb.append("All ").append(scan.suppressedCount())
              .append(" raw matches were vendor/runtime API noise and were suppressed.\n\n");
            sb.append("Consider investigating via:\n");
            sb.append("- `packagedefinition.txt` (if present) for chunk structure\n");
            sb.append("- Glacier/archive tooling to inspect .rpkg contents directly\n");
            sb.append("- Save inventory for player data and backup targets\n\n");
        }

        // High-confidence leads
        appendHintSection(sb, "High-confidence leads", scan.highHints());

        // Medium-confidence leads
        appendHintSection(sb, "Medium-confidence leads", scan.mediumHints());

        // Low-confidence leads (summarised)
        List<BinaryStringHint> low = scan.lowHints();
        if (!low.isEmpty()) {
            sb.append("## Low-confidence leads\n\n");
            sb.append(low.size()).append(" low-confidence hit(s) in non-vendor files. ");
            sb.append("These may be incidental string matches.\n\n");
            Map<String, Long> byKeyword = low.stream()
                .collect(Collectors.groupingBy(BinaryStringHint::keyword, Collectors.counting()));
            byKeyword.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append("- `").append(e.getKey()).append("` — ")
                    .append(e.getValue()).append(" match(es)\n"));
            sb.append('\n');
        }

        // Suppressed noise summary (by file, not line-by-line)
        List<BinaryStringHint> noisy = scan.noisyHints();
        if (!noisy.isEmpty()) {
            sb.append("## Suppressed noise summary\n\n");
            sb.append("The following files produced noise hits that were suppressed.\n");
            sb.append("They are **not** useful game modding leads.\n\n");
            sb.append("| File | Category | Suppressed hits | Reason |\n");
            sb.append("|------|----------|-----------------|--------|\n");

            Map<String, List<BinaryStringHint>> byFile = noisy.stream()
                .collect(Collectors.groupingBy(BinaryStringHint::relativePath));
            byFile.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<BinaryStringHint>> e) ->
                    e.getValue().size()).reversed())
                .forEach(e -> {
                    String topReason = e.getValue().stream()
                        .map(BinaryStringHint::suppressionReason)
                        .filter(r -> r != null && !r.isBlank())
                        .findFirst().orElse("vendor/runtime string");
                    sb.append("| `").append(e.getKey()).append("` | ")
                      .append(e.getValue().isEmpty() ? "" : e.getValue().get(0).sourceCategory()).append(" | ")
                      .append(e.getValue().size()).append(" | ")
                      .append(topReason).append(" |\n");
                });
            sb.append('\n');
        }

        sb.append("---\n");
        sb.append("*Extracted via ASCII string sampling of first/last 16 MB per file.*\n");

        writeFile(outputFile, sb);
    }

    private static void appendHintSection(StringBuilder sb, String title, List<BinaryStringHint> hints) {
        sb.append("## ").append(title).append("\n\n");
        if (hints.isEmpty()) {
            sb.append("None.\n\n");
            return;
        }
        sb.append("| File | Keyword | Offset | Context | Explanation |\n");
        sb.append("|------|---------|--------|---------|-------------|\n");
        hints.stream()
            .sorted(Comparator.comparing(BinaryStringHint::relativePath)
                .thenComparingLong(BinaryStringHint::fileOffset))
            .forEach(h -> sb.append("| `").append(h.relativePath()).append("` | `")
                .append(h.keyword()).append("` | ").append(h.fileOffset()).append(" | `")
                .append(h.context().replace("|", "\\|")).append("` | ")
                .append(h.confidenceExplanation() != null ? h.confidenceExplanation() : "").append(" |\n"));
        sb.append('\n');
    }

    private static void writeFile(Path outputFile, StringBuilder sb) throws IOException {
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
