package com.acltabontabon.modscope.triage;

import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.library.GameSource;
import com.acltabontabon.modscope.recommendation.RecommendationType;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record GameTriageResult(
    String gameId,
    String gameName,
    Path installPath,
    GameSource source,
    EngineFamily detectedEngine,
    int engineConfidence,
    List<String> archiveFormats,
    SurfaceLevel configSurface,
    SurfaceLevel saveSurface,
    SurfaceLevel looseFileSurface,
    SurfaceLevel packedArchiveSurface,
    ExternalToolDependency externalToolDependency,
    RecommendationType recommendedAction,
    int scanConfidence,
    Instant lastScannedAt,
    List<Badge> badges,
    int filesScanned,
    long bytesScanned,
    String engineRationale
) {}
