package com.acltabontabon.modscope.engine;

import java.util.List;

public record EngineDetectionResult(
    EngineFamily primary,
    int confidence,
    List<EngineSignal> signals
) {
    public static EngineDetectionResult unknown() {
        return new EngineDetectionResult(EngineFamily.UNKNOWN, 0, List.of());
    }

    public boolean isKnown() {
        return primary != EngineFamily.UNKNOWN;
    }
}
