package com.acltabontabon.modscope.library;

import com.acltabontabon.modscope.storage.AppPaths;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Generic identity for any installed game discovered by ModScope, irrespective of store.
 * Source-specific details (Steam app manifest, etc.) live in metadata.
 */
public record DetectedGame(
    String id,
    String displayName,
    GameSource source,
    String appId,
    Path installPath,
    List<Path> executableCandidates,
    Path manifestPath,
    Instant detectedAt,
    int confidence,
    Map<String, String> metadata
) {
    public String safeId() {
        return AppPaths.safeGameId(displayName, appId);
    }
}
