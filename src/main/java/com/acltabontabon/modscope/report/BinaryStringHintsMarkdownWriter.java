package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
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
        List<BinaryStringHint> hints = result.binaryHints();
        StringBuilder sb = new StringBuilder();
        sb.append("# Binary String Hints\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");
        sb.append("QoL-relevant strings extracted from binary/archive files via sampling.\n\n");

        if (hints.isEmpty()) {
            sb.append("No binary string hints found.\n");
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
            return;
        }

        sb.append("Total hits: **").append(hints.size()).append("**\n\n");

        // Group by file
        Map<String, List<BinaryStringHint>> byFile = hints.stream()
            .collect(Collectors.groupingBy(BinaryStringHint::relativePath));

        byFile.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, List<BinaryStringHint>> e) ->
                e.getValue().size()).reversed())
            .forEach(entry -> {
                sb.append("## `").append(entry.getKey()).append("` (")
                  .append(entry.getValue().size()).append(" hit(s))\n\n");
                sb.append("| Keyword | Offset | Context |\n|---------|--------|--------|\n");
                entry.getValue().stream()
                    .sorted(Comparator.comparingLong(BinaryStringHint::fileOffset))
                    .forEach(h -> sb.append("| `").append(h.keyword()).append("` | ")
                        .append(h.fileOffset()).append(" | `")
                        .append(h.context().replace("|", "\\|")).append("` |\n"));
                sb.append('\n');
            });

        sb.append("---\n");
        sb.append("*Extracted via ASCII string scanning of first/last 16 MB of each file.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
