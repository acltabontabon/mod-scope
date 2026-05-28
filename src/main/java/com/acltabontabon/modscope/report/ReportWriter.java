package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReportWriter {

    private ReportWriter() {}

    public static void write(Path outputDir, ScanResult result) throws IOException {
        Files.createDirectories(outputDir);
        ScanSummaryMarkdownWriter.write(outputDir.resolve("scan-summary.md"), result);
        FileInventoryJsonWriter.write(outputDir.resolve("file-inventory.json"), result);
        CandidatesMarkdownWriter.write(outputDir.resolve("candidates.md"), result);
        TextHintsMarkdownWriter.write(outputDir.resolve("text-hints.md"), result);
        SaveLocationsMarkdownWriter.write(outputDir.resolve("save-locations.md"), result);
        SaveInventoryMarkdownWriter.write(outputDir.resolve("save-inventory.md"), result);
        if (result.packageDefinition().found()) {
            PackageDefinitionMarkdownWriter.write(outputDir.resolve("package-definition-analysis.md"), result);
        }
        if (!result.binaryScan().allHints().isEmpty()) {
            BinaryStringHintsMarkdownWriter.write(outputDir.resolve("binary-string-hints.md"), result);
        }
    }
}
