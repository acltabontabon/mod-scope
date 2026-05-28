package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.save.SaveFileEntry;
import com.acltabontabon.modscope.util.FileSizeFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SaveInventoryMarkdownWriter {

    private SaveInventoryMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Save File Inventory\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");

        if (result.saveInventory().isEmpty()) {
            sb.append("No save files found.\n");
        } else {
            sb.append("| File | Size | Last Modified |\n");
            sb.append("|------|------|---------------|\n");
            for (SaveFileEntry entry : result.saveInventory()) {
                sb.append("| `").append(entry.displayName()).append("` | ")
                  .append(FileSizeFormatter.format(entry.sizeBytes())).append(" | ")
                  .append(entry.lastModified()).append(" |\n");
            }
        }

        sb.append('\n');
        sb.append("---\n");
        sb.append("*READ-ONLY SCAN — ModScope did not modify any save files.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
