package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.util.FileSizeFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class CandidatesMarkdownWriter {

    private CandidatesMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Modding Surface Candidates\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append("\n\n");

        appendSection(sb, "Config-like files", result.files(), FileCategory.CONFIG);
        appendSection(sb, "Archive / packed resource files", result.files(), FileCategory.ARCHIVE);
        appendSection(sb, "Video / intro files", result.files(), FileCategory.VIDEO);
        appendSection(sb, "Localization files", result.files(), FileCategory.LOCALIZATION);
        appendSection(sb, "Text / JSON / script files", result.files(), FileCategory.TEXT);

        sb.append("## Suspiciously readable files (OTHER category)\n\n");
        List<FileEntry> others = result.files().stream()
            .filter(e -> e.category() == FileCategory.OTHER && e.sizeBytes() < 1024 * 1024)
            .limit(50)
            .toList();
        if (others.isEmpty()) {
            sb.append("None.\n\n");
        } else {
            for (FileEntry e : others) {
                sb.append("- `").append(e.relativePath()).append("` (")
                  .append(FileSizeFormatter.format(e.sizeBytes())).append(")\n");
            }
            sb.append('\n');
        }

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void appendSection(StringBuilder sb, String title, List<FileEntry> files, FileCategory category) {
        List<FileEntry> matches = files.stream().filter(e -> e.category() == category).toList();
        sb.append("## ").append(title).append("\n\n");
        if (matches.isEmpty()) {
            sb.append("None detected.\n\n");
            return;
        }
        for (FileEntry e : matches) {
            sb.append("- `").append(e.relativePath()).append("` (")
              .append(FileSizeFormatter.format(e.sizeBytes())).append(")\n");
        }
        sb.append('\n');
    }
}
