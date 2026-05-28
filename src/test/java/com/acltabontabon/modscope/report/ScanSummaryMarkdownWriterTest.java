package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.core.ScanOptions;
import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.scan.HintMatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScanSummaryMarkdownWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesScanSummaryFile() throws IOException {
        ScanOptions options = new ScanOptions("007-first-light", null, tempDir, ScanMode.STANDARD);
        List<FileEntry> files = List.of(
            new FileEntry("Retail/Game.exe", FileCategory.EXECUTABLE, "exe", 1024, "2026-01-01T00:00:00Z", null, "exceeds hash limit"),
            new FileEntry("Config/settings.cfg", FileCategory.CONFIG, "cfg", 512, "2026-01-01T00:00:00Z", "abc123", null)
        );
        List<HintMatch> hints = List.of(
            new HintMatch("Config/settings.cfg", 3, "motionblur", "motionblur=true", HintMatch.Confidence.HIGH)
        );
        ScanResult result = new ScanResult(Optional.empty(), List.of(), files, hints, tempDir, "2026-05-28T10:00:00Z", options);

        Path outFile = tempDir.resolve("scan-summary.md");
        ScanSummaryMarkdownWriter.write(outFile, result);

        assertTrue(Files.exists(outFile));
        String content = Files.readString(outFile);
        assertTrue(content.contains("# Scan Summary"));
        assertTrue(content.contains("STANDARD"));
        assertTrue(content.contains("motionblur"));
        assertTrue(content.contains("READ-ONLY SCAN"));
        assertTrue(content.contains("EXECUTABLE"));
    }
}
