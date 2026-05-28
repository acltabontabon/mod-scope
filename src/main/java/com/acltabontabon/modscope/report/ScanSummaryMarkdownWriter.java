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
import java.util.LinkedHashMap;
import java.util.List;
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
            sb.append("No QoL hints found.\n");
        } else {
            Map<String, Long> keywordCounts = result.hints().stream()
                .collect(Collectors.groupingBy(HintMatch::keyword, Collectors.counting()));
            keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> sb.append("- **").append(e.getKey()).append("**: found in ").append(e.getValue()).append(" match(es)\n"));
        }
        sb.append('\n');

        sb.append("---\n");
        sb.append("*READ-ONLY SCAN — ModScope did not modify any game files.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
