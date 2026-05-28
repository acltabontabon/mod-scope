package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.recommendation.Recommendation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecommendationsMarkdownWriter {

    private RecommendationsMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Recommendations\n\n");
        sb.append("> READ-ONLY SCAN — ModScope did not modify, rename, delete, patch, overwrite, inject, or execute game files.\n\n");

        if (result.recommendations() == null || result.recommendations().isEmpty()) {
            sb.append("No recommendations were generated for this scan.\n");
        } else {
            int n = 1;
            for (Recommendation rec : result.recommendations()) {
                sb.append("## ").append(n++).append(". ").append(rec.title()).append('\n');
                sb.append("- **Type:** ").append(rec.type()).append('\n');
                sb.append("- **Confidence:** ").append(rec.confidence()).append("%\n");
                sb.append("- **Risk:** ").append(rec.riskLevel()).append('\n');
                sb.append('\n');
                sb.append("**Reason.** ").append(rec.reason()).append("\n\n");
                sb.append("**Suggested action.** ").append(rec.suggestedAction()).append("\n\n");
                if (rec.relatedPaths() != null && !rec.relatedPaths().isEmpty()) {
                    sb.append("**Related paths:**\n");
                    for (String p : rec.relatedPaths()) sb.append("- `").append(p).append("`\n");
                    sb.append('\n');
                }
            }
        }

        sb.append("---\n");
        sb.append("*Read external-tooling recommendations as listing first. Do not extract/rebuild/replace archives without backups and a clear modding workflow.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
