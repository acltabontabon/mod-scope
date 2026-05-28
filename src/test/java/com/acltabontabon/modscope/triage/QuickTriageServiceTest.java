package com.acltabontabon.modscope.triage;

import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.library.GameSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QuickTriageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void triagesGlacierLayoutAsArchiveHeavy() throws IOException {
        Path install = tempDir.resolve("game");
        Files.createDirectories(install.resolve("Runtime"));
        Files.writeString(install.resolve("Runtime/packagedefinition.txt"), "[chunk0]\n");
        for (int i = 0; i < 60; i++) {
            Files.writeString(install.resolve("Runtime/chunk" + i + ".rpkg"), "fake");
        }
        Files.writeString(install.resolve("Glacier2.exe"), "x");

        var game = detected(install, "007 First Light");
        var outcome = new QuickTriageService().triage(game);
        assertTrue(outcome.success());

        var r = outcome.result();
        assertEquals(EngineFamily.GLACIER, r.detectedEngine());
        assertEquals(SurfaceLevel.HIGH, r.packedArchiveSurface());
        assertTrue(r.badges().contains(Badge.ARCHIVE_HEAVY));
        assertNotEquals(ExternalToolDependency.NONE, r.externalToolDependency());
    }

    @Test
    void triagesConfigRichLayoutAsGoodFirstTarget() throws IOException {
        Path install = tempDir.resolve("game");
        Files.createDirectories(install);
        Files.writeString(install.resolve("hl2.exe"), "x");
        Files.writeString(install.resolve("gameinfo.txt"), "GameInfo");
        for (int i = 0; i < 60; i++) {
            Files.writeString(install.resolve("config" + i + ".cfg"), "fov=90");
        }

        var game = detected(install, "Test Source Game");
        var outcome = new QuickTriageService().triage(game);
        assertTrue(outcome.success());
        var r = outcome.result();
        assertEquals(EngineFamily.SOURCE, r.detectedEngine());
        assertEquals(SurfaceLevel.HIGH, r.configSurface());
        assertTrue(r.badges().contains(Badge.CONFIGS_FOUND));
    }

    @Test
    void missingInstallPathFailsGracefully() {
        var game = detected(tempDir.resolve("nope"), "Ghost");
        var outcome = new QuickTriageService().triage(game);
        assertFalse(outcome.success());
        assertNotNull(outcome.error());
    }

    private static DetectedGame detected(Path install, String name) {
        return new DetectedGame(
            "steam-test", name, GameSource.STEAM, "0",
            install, List.of(), null, Instant.now(), 90, Map.of()
        );
    }
}
