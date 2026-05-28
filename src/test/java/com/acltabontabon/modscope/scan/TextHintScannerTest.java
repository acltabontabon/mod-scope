package com.acltabontabon.modscope.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextHintScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void findsMotionBlurHint() throws IOException {
        Path cfg = tempDir.resolve("settings.cfg");
        Files.writeString(cfg, """
            [graphics]
            motionblur=true
            fov=90
            """, StandardCharsets.UTF_8);

        List<HintMatch> matches = TextHintScanner.scan(cfg, "cfg");

        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(m -> m.keyword().equals("motionblur")));
        assertTrue(matches.stream().anyMatch(m -> m.keyword().equals("fov")));
    }

    @Test
    void assignsHighConfidenceForConfigFiles() throws IOException {
        Path ini = tempDir.resolve("game.ini");
        Files.writeString(ini, "blur=1\n", StandardCharsets.UTF_8);

        List<HintMatch> matches = TextHintScanner.scan(ini, "ini");
        assertFalse(matches.isEmpty());
        assertEquals(HintMatch.Confidence.HIGH, matches.get(0).confidence());
    }

    @Test
    void skipsNonReadableExtensions() throws IOException {
        Path pak = tempDir.resolve("data.pak");
        Files.writeString(pak, "motionblur=1\n");

        List<HintMatch> matches = TextHintScanner.scan(pak, "pak");
        assertTrue(matches.isEmpty());
    }

    @Test
    void returnsEmptyForEmptyFile() throws IOException {
        Path empty = tempDir.resolve("empty.cfg");
        Files.writeString(empty, "");

        List<HintMatch> matches = TextHintScanner.scan(empty, "cfg");
        assertTrue(matches.isEmpty());
    }

    @Test
    void capturesLineNumberCorrectly() throws IOException {
        Path cfg = tempDir.resolve("test.cfg");
        Files.writeString(cfg, """
            resolution=1920x1080
            vignette=false
            shadows=high
            """);

        List<HintMatch> matches = TextHintScanner.scan(cfg, "cfg");
        assertTrue(matches.stream().anyMatch(m -> m.keyword().equals("vignette") && m.lineNumber() == 2));
    }
}
