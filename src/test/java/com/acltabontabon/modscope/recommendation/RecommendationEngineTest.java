package com.acltabontabon.modscope.recommendation;

import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.library.GameSource;
import com.acltabontabon.modscope.triage.Badge;
import com.acltabontabon.modscope.triage.ExternalToolDependency;
import com.acltabontabon.modscope.triage.GameTriageResult;
import com.acltabontabon.modscope.triage.SurfaceLevel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEngineTest {

    @Test
    void goodFirstTargetWhenConfigsAndKnownEngineAndLooseSurface() {
        var triage = triage(EngineFamily.UNREAL, 80,
            SurfaceLevel.HIGH, SurfaceLevel.NONE, SurfaceLevel.HIGH, SurfaceLevel.LOW,
            ExternalToolDependency.NONE, 85, List.of(Badge.GOOD_FIRST_TARGET));
        var recs = RecommendationEngine.evaluate(triage);
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.GOOD_FIRST_MOD_TARGET));
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.INSPECT_CONFIGS));
    }

    @Test
    void archiveHeavyKnownEngineGetsInspectArchivesAndExternalTool() {
        var triage = triage(EngineFamily.GLACIER, 90,
            SurfaceLevel.NONE, SurfaceLevel.NONE, SurfaceLevel.LOW, SurfaceLevel.HIGH,
            ExternalToolDependency.REQUIRED, 85, List.of(Badge.ARCHIVE_HEAVY));
        var recs = RecommendationEngine.evaluate(triage);
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.INSPECT_ARCHIVES));
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.SET_UP_EXTERNAL_TOOL));
        assertTrue(recs.stream().anyMatch(r -> r.reason().contains("Glacier")),
            "Glacier-specific tool name should appear in the reason");
    }

    @Test
    void savePresenceProducesBuildSaveBackup() {
        var triage = triage(EngineFamily.UNREAL, 80,
            SurfaceLevel.MEDIUM, SurfaceLevel.MEDIUM, SurfaceLevel.MEDIUM, SurfaceLevel.LOW,
            ExternalToolDependency.NONE, 85, List.of(Badge.SAVES_FOUND));
        var recs = RecommendationEngine.evaluate(triage);
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.BUILD_SAVE_BACKUP));
    }

    @Test
    void lowConfidenceFirstRecommendsDeepScan() {
        var triage = triage(EngineFamily.UNKNOWN, 0,
            SurfaceLevel.NONE, SurfaceLevel.NONE, SurfaceLevel.LOW, SurfaceLevel.LOW,
            ExternalToolDependency.NONE, 35, List.of(Badge.LOW_CONFIDENCE));
        var recs = RecommendationEngine.evaluate(triage);
        assertFalse(recs.isEmpty());
        assertEquals(RecommendationType.RUN_DEEP_SCAN, recs.get(0).type());
    }

    @Test
    void unknownEngineWithNothingInterestingFlagsLowValue() {
        var triage = triage(EngineFamily.UNKNOWN, 0,
            SurfaceLevel.NONE, SurfaceLevel.NONE, SurfaceLevel.NONE, SurfaceLevel.NONE,
            ExternalToolDependency.NONE, 80, List.of(Badge.UNKNOWN_ENGINE));
        var recs = RecommendationEngine.evaluate(triage);
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.LOW_VALUE_TARGET));
    }

    @Test
    void unknownEngineButPromisingContentFlagsManualReview() {
        var triage = triage(EngineFamily.UNKNOWN, 0,
            SurfaceLevel.HIGH, SurfaceLevel.NONE, SurfaceLevel.MEDIUM, SurfaceLevel.LOW,
            ExternalToolDependency.NONE, 80, List.of(Badge.UNKNOWN_ENGINE));
        var recs = RecommendationEngine.evaluate(triage);
        assertTrue(recs.stream().anyMatch(r -> r.type() == RecommendationType.MANUAL_REVIEW));
    }

    @Test
    void externalToolRecommendationIncludesReadOnlyCaveat() {
        var triage = triage(EngineFamily.GLACIER, 90,
            SurfaceLevel.NONE, SurfaceLevel.NONE, SurfaceLevel.LOW, SurfaceLevel.HIGH,
            ExternalToolDependency.REQUIRED, 80, List.of());
        var recs = RecommendationEngine.evaluate(triage);
        var ext = recs.stream().filter(r -> r.type() == RecommendationType.SET_UP_EXTERNAL_TOOL).findFirst();
        assertTrue(ext.isPresent());
        assertTrue(ext.get().reason().toLowerCase().contains("listing first"));
    }

    private static GameTriageResult triage(EngineFamily engine, int engineConf,
                                           SurfaceLevel cfg, SurfaceLevel save, SurfaceLevel loose,
                                           SurfaceLevel packed, ExternalToolDependency ext,
                                           int scanConf, List<Badge> badges) {
        return new GameTriageResult(
            "id", "Test Game", Path.of("/tmp/test"), GameSource.STEAM,
            engine, engineConf, List.of(),
            cfg, save, loose, packed, ext,
            RecommendationType.MANUAL_REVIEW, scanConf, Instant.now(),
            badges, 100, 1024L, "test"
        );
    }
}
