package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.util.FileSizeFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CandidatesMarkdownWriter {

    private CandidatesMarkdownWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Modding Surface Candidates\n\n");
        sb.append("Scanned: ").append(result.scannedAt()).append('\n');
        sb.append("Modding surface score: **").append(result.surfaceScore()).append("**\n\n");

        // Engine and known toolchain context
        var eng = result.engineDetection();
        if (eng.isKnown()) {
            sb.append("## Engine context\n\n");
            sb.append("Detected engine: **").append(eng.primary())
              .append("** (confidence ").append(eng.confidence()).append("%)\n\n");
            appendToolchainNotes(sb, eng.primary());
        }

        // Archive surface — most useful for archive-heavy games
        List<FileEntry> archives = result.files().stream()
            .filter(e -> e.category() == FileCategory.ARCHIVE)
            .sorted(Comparator.comparingLong(FileEntry::sizeBytes).reversed())
            .toList();

        sb.append("## Archives / packed resources\n\n");
        if (archives.isEmpty()) {
            sb.append("No archive files detected.\n\n");
        } else {
            Map<String, Long> byExt = archives.stream()
                .collect(Collectors.groupingBy(FileEntry::extension, Collectors.counting()));
            sb.append("Total: ").append(archives.size()).append(" archive(s) across ")
              .append(byExt.size()).append(" format(s): ")
              .append(byExt.entrySet().stream()
                  .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                  .map(e -> e.getKey() + " ×" + e.getValue())
                  .collect(Collectors.joining(", ")))
              .append("\n\n");
            sb.append("Largest archives:\n\n");
            archives.stream().limit(20).forEach(e ->
                sb.append("- `").append(e.relativePath()).append("` (")
                  .append(FileSizeFormatter.format(e.sizeBytes())).append(")\n")
            );
            sb.append('\n');
        }

        // Package definition
        if (result.packageDefinition().found()) {
            sb.append("## Package definition (Glacier chunk manifest)\n\n");
            sb.append("Found `packagedefinition.txt` with **")
              .append(result.packageDefinition().chunkCount()).append("** chunk entries.\n\n");
            sb.append("See `package-definition-analysis.md` for the full breakdown.\n\n");
        }

        // Loose modding surface
        appendSection(sb, "Config-like files", result.files(), FileCategory.CONFIG);
        appendSection(sb, "Text / JSON / script files", result.files(), FileCategory.TEXT);
        appendSection(sb, "Localization files", result.files(), FileCategory.LOCALIZATION);
        appendSection(sb, "Video / intro files", result.files(), FileCategory.VIDEO);

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

    private static void appendToolchainNotes(StringBuilder sb, EngineFamily family) {
        String note = switch (family) {
            case UNREAL -> """
                Known toolchain: UE4SS/UE5SS (Lua/C++ scripting), UnrealPak/repak (pak extraction),
                UAssetAPI/UAssetGUI (asset editing). .pak files are the primary mod target.
                """;
            case GLACIER -> """
                Known toolchain: RPKG Tool (extract/rebuild .rpkg chunks), Glacier Modding Framework,
                Simple Mod Framework. packagedefinition.txt lists all chunk assignments.
                """;
            case CREATION -> """
                Known toolchain: xEdit (TES5Edit/FO4Edit), Creation Kit, BAE (BA2 extractor),
                BSArch. .ba2/.bsa archives are the primary mod target.
                """;
            case FROSTBITE -> "Known toolchain: FrostyEditor (limited support depending on title).\n";
            case ANVIL -> "Known toolchain: .forge files — limited public tools available.\n";
            default -> null;
        };
        if (note != null) sb.append(note).append('\n');
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
