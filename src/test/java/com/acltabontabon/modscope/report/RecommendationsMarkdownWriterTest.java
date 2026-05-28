package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ModdingSurfaceScore;
import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.core.ScanOptions;
import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.engine.EngineDetectionResult;
import com.acltabontabon.modscope.recommendation.Recommendation;
import com.acltabontabon.modscope.recommendation.RecommendationType;
import com.acltabontabon.modscope.recommendation.RiskLevel;
import com.acltabontabon.modscope.scan.BinaryScanPolicy;
import com.acltabontabon.modscope.scan.BinaryScanResult;
import com.acltabontabon.modscope.scan.PackageDefinitionAnalysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationsMarkdownWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void rendersTitleReasonAndSafetyBanner() throws IOException {
        var recs = List.of(
            new Recommendation(
                RecommendationType.INSPECT_ARCHIVES,
                "Archive listing recommended",
                "Most game content appears to be packed into large package files.",
                80,
                "List archive contents read-only first.",
                RiskLevel.LOW,
                List.of()
            )
        );
        var result = scanResult(recs);

        Path out = tempDir.resolve("recommendations.md");
        RecommendationsMarkdownWriter.write(out, result);
        String md = Files.readString(out);

        assertTrue(md.contains("# Recommendations"));
        assertTrue(md.contains("Archive listing recommended"));
        assertTrue(md.contains("Most game content appears to be packed"));
        assertTrue(md.contains("READ-ONLY SCAN"));
        assertTrue(md.toLowerCase().contains("listing first"));
    }

    @Test
    void emptyRecommendationsStillWritesBanner() throws IOException {
        Path out = tempDir.resolve("recommendations.md");
        RecommendationsMarkdownWriter.write(out, scanResult(List.of()));
        String md = Files.readString(out);
        assertTrue(md.contains("READ-ONLY SCAN"));
        assertTrue(md.contains("No recommendations"));
    }

    private ScanResult scanResult(List<Recommendation> recs) {
        var opts = new ScanOptions(null, null, tempDir, ScanMode.STANDARD, BinaryScanPolicy.conservative());
        return new ScanResult(
            Optional.empty(), List.of(), List.of(), List.of(), List.of(),
            EngineDetectionResult.unknown(), PackageDefinitionAnalysis.notFound(),
            BinaryScanResult.empty(BinaryScanPolicy.conservative()),
            ModdingSurfaceScore.NONE, recs, tempDir, "2026-01-01T00:00:00Z", opts
        );
    }
}
