package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.scan.HintMatch;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.util.FileSizeFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public final class ScanSummaryMarkdownWriter {

    private ScanSummaryMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Scan Summary\n\n");
        sb.append("- **Scan date:** ").append(result.scannedAt()).append('\n');

        result.install().ifPresentOrElse(
            install -> {
                sb.append("- **Game:** ").append(install.profile().displayName()).append('\n');
                sb.append("- **Install path:** `").append(install.installPath()).append("`\n");
            },
            () -> sb.append("- **Game:** not detected\n")
        );

        sb.append("- **Scan mode:** ").append(result.options().mode()).append('\n');
        sb.append("- **Total files:** ").append(result.files().size()).append('\n');

        var eng = result.engineDetection();
        if (eng.isKnown()) {
            sb.append("- **Engine:** ").append(eng.primary())
              .append(" (confidence ").append(eng.confidence()).append("%)\n");
        } else {
            sb.append("- **Engine:** unknown\n");
        }

        sb.append("- **Modding surface:** ").append(result.surfaceScore()).append('\n');
        sb.append('\n');

        sb.append("## File counts\n\n");
        sb.append("| Category | Count |\n|----------|-------|\n");
        Map<FileCategory, Long> counts = result.files().stream()
            .collect(Collectors.groupingBy(FileEntry::category, Collectors.counting()));
        for (FileCategory cat : FileCategory.values()) {
            long count = counts.getOrDefault(cat, 0L);
            if (count > 0) sb.append("| ").append(cat).append(" | ").append(count).append(" |\n");
        }
        sb.append('\n');

        if (eng.isKnown() && !eng.signals().isEmpty()) {
            sb.append("## Engine detection signals\n\n");
            for (var signal : eng.signals()) {
                sb.append("- **").append(signal.family()).append("** (weight ").append(signal.weight())
                  .append("): ").append(signal.evidence()).append('\n');
            }
            sb.append('\n');
        }

        if (result.packageDefinition().found()) {
            sb.append("## Package definition\n\n");
            sb.append("- **Assembly path:** `").append(result.packageDefinition().assemblyPath()).append("`\n");
            sb.append("- **Chunks:** ").append(result.packageDefinition().chunkCount()).append('\n');
            sb.append('\n');
        }

        sb.append("## Save candidates\n\n");
        if (result.saves().isEmpty()) {
            sb.append("No save candidates detected.\n");
        } else {
            for (SaveCandidate save : result.saves()) {
                String status = save.exists() ? "exists" : "not found";
                sb.append("- `").append(save.path()).append("` — ").append(status);
                if (save.exists() && save.sizeBytes() > 0) {
                    sb.append(" (").append(FileSizeFormatter.format(save.sizeBytes())).append(')');
                }
                sb.append('\n');
            }
        }
        sb.append('\n');

        sb.append("## QoL investigation leads\n\n");
        if (result.hints().isEmpty()) {
            sb.append("No QoL hints found in text/config files.\n");
        } else {
            Map<String, Long> keywordCounts = result.hints().stream()
                .collect(Collectors.groupingBy(HintMatch::keyword, Collectors.counting()));
            keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> sb.append("- **").append(e.getKey()).append("**: found in ")
                    .append(e.getValue()).append(" match(es)\n"));
        }
        if (!result.binaryHints().isEmpty()) {
            sb.append('\n');
            sb.append("Binary string scanner found **").append(result.binaryHints().size())
              .append("** QoL keyword hit(s) in binary/archive files. See `binary-string-hints.md`.\n");
        }
        sb.append('\n');

        sb.append("---\n");
        sb.append("*READ-ONLY SCAN — ModScope did not modify any game files.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
