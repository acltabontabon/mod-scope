package com.acltabontabon.modscope.history;

import com.acltabontabon.modscope.library.GameSource;

import java.util.List;

public record ScanHistoryEntry(
    String gameId,
    String displayName,
    String installPath,
    GameSource source,
    String lastScanAt,
    String lastScanMode,
    String engineHint,
    String surfaceScore,
    String reportPath,
    String recommendedAction,
    List<String> badges,
    List<String> warnings
) {}
