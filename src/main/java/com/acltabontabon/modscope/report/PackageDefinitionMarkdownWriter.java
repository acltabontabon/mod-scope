package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.PackageDefinitionEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PackageDefinitionMarkdownWriter {

    private PackageDefinitionMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        var analysis = result.packageDefinition();
        StringBuilder sb = new StringBuilder();
        sb.append("# Package Definition Analysis\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");

        if (!analysis.found()) {
            sb.append("No `packagedefinition.txt` found.\n");
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
            return;
        }

        if (analysis.assemblyPath() != null) {
            sb.append("**Assembly path:** `").append(analysis.assemblyPath()).append("`\n\n");
        }
        sb.append("**Total chunks:** ").append(analysis.chunkCount()).append("\n\n");

        // Group entries by section
        Map<String, List<PackageDefinitionEntry>> bySect = analysis.entries().stream()
            .collect(Collectors.groupingBy(PackageDefinitionEntry::section,
                LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<PackageDefinitionEntry>> sect : bySect.entrySet()) {
            sb.append("## @").append(sect.getKey()).append("\n\n");
            sb.append("| Chunk | Resource |\n|-------|----------|\n");
            for (PackageDefinitionEntry entry : sect.getValue()) {
                sb.append("| `").append(entry.chunkName()).append("` | `")
                  .append(entry.resourcePath()).append("` |\n");
            }
            sb.append('\n');
        }

        sb.append("---\n");
        sb.append("*READ-ONLY SCAN — ModScope did not modify any game files.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
