package com.acltabontabon.modscope.triage;

import com.acltabontabon.modscope.engine.EngineDetectionResult;
import com.acltabontabon.modscope.engine.EngineFamily;

import java.util.ArrayList;
import java.util.List;

public final class BadgeCalculator {

    private BadgeCalculator() {}

    public static List<Badge> compute(
            EngineDetectionResult engine,
            SurfaceLevel configSurface,
            SurfaceLevel saveSurface,
            SurfaceLevel looseFileSurface,
            SurfaceLevel packedArchiveSurface,
            ExternalToolDependency external,
            int scanConfidence) {

        List<Badge> badges = new ArrayList<>();

        if (looseFileSurface.atLeast(SurfaceLevel.MEDIUM)) badges.add(Badge.LOOSE_FILES);
        if (packedArchiveSurface.atLeast(SurfaceLevel.HIGH)) badges.add(Badge.ARCHIVE_HEAVY);
        if (configSurface.atLeast(SurfaceLevel.LOW)) badges.add(Badge.CONFIGS_FOUND);
        if (saveSurface.atLeast(SurfaceLevel.LOW)) badges.add(Badge.SAVES_FOUND);
        if (external != ExternalToolDependency.NONE) badges.add(Badge.EXTERNAL_TOOL_NEEDED);
        if (engine.primary() == EngineFamily.UNKNOWN) badges.add(Badge.UNKNOWN_ENGINE);
        if (scanConfidence >= 75) badges.add(Badge.HIGH_CONFIDENCE);
        else if (scanConfidence < 50) badges.add(Badge.LOW_CONFIDENCE);

        boolean knownEngine = engine.primary() != EngineFamily.UNKNOWN;
        boolean goodFirst = configSurface.atLeast(SurfaceLevel.MEDIUM)
            && looseFileSurface.atLeast(SurfaceLevel.MEDIUM)
            && knownEngine
            && external != ExternalToolDependency.REQUIRED;
        boolean hardTarget = packedArchiveSurface.atLeast(SurfaceLevel.HIGH)
            && configSurface == SurfaceLevel.NONE
            && (external == ExternalToolDependency.REQUIRED || !knownEngine);

        if (goodFirst) badges.add(Badge.GOOD_FIRST_TARGET);
        if (hardTarget) badges.add(Badge.HARD_TARGET);

        return List.copyOf(badges);
    }
}
