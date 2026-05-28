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
        sb.append("> READ-ONLY SCAN — ModScope did not modify, rename, delete, patch, overwrite, inject, or execute game files.\n\n");
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

        sb.append("## In plain English\n\n");
        appendPlainEnglishSummary(sb, result);
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
        if (!result.binaryScan().allHints().isEmpty()) {
            sb.append('\n');
            int useful = result.binaryScan().usefulCount();
            int suppressed = result.binaryScan().suppressedCount();
            if (useful > 0) {
                sb.append("Binary string scanner found **").append(useful)
                  .append("** useful QoL keyword hit(s) in game files. See `binary-string-hints.md`.\n");
            } else {
                sb.append("Binary string scanning found no high-confidence game-specific QoL strings. ")
                  .append(suppressed > 0 ? "Most matches (" + suppressed + ") were "
                      + "vendor/runtime API noise and were suppressed. " : "")
                  .append("See `binary-string-hints.md`.\n");
            }
        }
        sb.append('\n');

        if (result.recommendations() != null && !result.recommendations().isEmpty()) {
            sb.append("## Recommended next actions\n\n");
            int max = Math.min(3, result.recommendations().size());
            for (int i = 0; i < max; i++) {
                var r = result.recommendations().get(i);
                sb.append("- **").append(r.title()).append("** — ").append(r.reason()).append('\n');
            }
            sb.append("\nSee `recommendations.md` for the full ranked list.\n\n");
        }

        sb.append("---\n");
        sb.append("*READ-ONLY SCAN — ModScope did not modify any game files.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void appendPlainEnglishSummary(StringBuilder sb, ScanResult result) {
        long total = result.files().size();
        long archives = result.files().stream().filter(f -> f.category() == com.acltabontabon.modscope.scan.FileCategory.ARCHIVE).count();
        long configs = result.files().stream().filter(f ->
            f.category() == com.acltabontabon.modscope.scan.FileCategory.CONFIG
            || f.category() == com.acltabontabon.modscope.scan.FileCategory.TEXT).count();

        var eng = result.engineDetection();
        if (eng.isKnown()) {
            sb.append("- Likely ").append(eng.primary())
              .append(" package layout detected from the file signatures listed below.\n");
        } else {
            sb.append("- No known engine layout was detected. Inspection will need to be manual.\n");
        }

        if (archives > 0 && total > 0 && (double) archives / total > 0.4) {
            sb.append("- This game is archive-heavy. Most content appears to be stored in large package files. ")
              .append("Loose-file QoL mods may be limited.\n");
        } else if (archives > 0) {
            sb.append("- Some packed archives are present; loose-file modding is still possible alongside.\n");
        }

        if (configs == 0) {
            sb.append("- No loose config files were found. Simple ini/json tweaks are unlikely from the install folder.\n");
        } else if (configs < 10) {
            sb.append("- A small number of loose config files exist; targeted tweaks may be possible.\n");
        } else {
            sb.append("- Many loose config/text files exist. This is a strong candidate for config-only mods.\n");
        }

        if (result.saves().stream().anyMatch(s -> s.exists() && s.sizeBytes() > 0)) {
            sb.append("- Save data was detected. A simple save-backup workflow is a realistic first quality-of-life feature.\n");
        }
    }
}
