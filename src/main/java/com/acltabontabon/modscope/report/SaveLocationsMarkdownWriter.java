package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.util.FileSizeFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SaveLocationsMarkdownWriter {

    private SaveLocationsMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Save Locations\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");

        if (result.saves().isEmpty()) {
            sb.append("No save candidates detected.\n\n");
            sb.append("Steam userdata directory may not have been found, or the game has not been run yet.\n");
        } else {
            sb.append("## Detected candidates\n\n");
            sb.append("| Path | Status | Size |\n|------|--------|------|\n");
            for (SaveCandidate save : result.saves()) {
                String status = save.exists() ? "**exists**" : "not found";
                String size = save.exists() ? FileSizeFormatter.format(save.sizeBytes()) : "-";
                sb.append("| `").append(save.path()).append("` | ")
                  .append(status).append(" | ")
                  .append(size).append(" |\n");
            }
            sb.append('\n');

            long found = result.saves().stream().filter(SaveCandidate::exists).count();
            sb.append("Found: **").append(found).append("** / ").append(result.saves().size())
              .append(" candidates exist.\n");
        }

        sb.append('\n');
        sb.append("---\n");
        sb.append("*ModScope did not read or modify any save file contents.*\n");

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
