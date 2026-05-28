package com.acltabontabon.modscope.library;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectedGameTest {

    @Test
    void safeIdSlugifiesDisplayName() {
        var g = new DetectedGame("steam-3768760", "007: First Light", GameSource.STEAM,
            "3768760", Path.of("/tmp"), List.of(), null, Instant.now(), 90, Map.of());
        assertEquals("007-first-light", g.safeId());
    }

    @Test
    void safeIdFallsBackToAppIdWhenNameEmpty() {
        var g = new DetectedGame("steam-12345", "!!!", GameSource.STEAM,
            "12345", Path.of("/tmp"), List.of(), null, Instant.now(), 50, Map.of());
        assertEquals("steam-12345", g.safeId());
    }
}
