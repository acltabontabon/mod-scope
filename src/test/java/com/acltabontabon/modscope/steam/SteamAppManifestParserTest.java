package com.acltabontabon.modscope.steam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SteamAppManifestParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesValidManifest() throws IOException {
        Path acfFile = tempDir.resolve("appmanifest_3768760.acf");
        Files.writeString(acfFile, """
            "AppState"
            {
            \t"appid"\t\t"3768760"
            \t"Universe"\t\t"1"
            \t"name"\t\t"007 First Light"
            \t"StateFlags"\t\t"4"
            \t"installdir"\t\t"007 First Light"
            }
            """, StandardCharsets.UTF_8);

        Optional<SteamAppManifest> result = SteamAppManifestParser.parse(acfFile);

        assertTrue(result.isPresent());
        assertEquals("3768760", result.get().appId());
        assertEquals("007 First Light", result.get().name());
        assertEquals("007 First Light", result.get().installDir());
        assertEquals("4", result.get().stateFlags());
    }

    @Test
    void returnsEmptyForMissingAppId() throws IOException {
        Path acfFile = tempDir.resolve("broken.acf");
        Files.writeString(acfFile, """
            "AppState"
            {
            \t"name"\t\t"Some Game"
            }
            """);

        Optional<SteamAppManifest> result = SteamAppManifestParser.parse(acfFile);
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyForNonExistentFile() {
        Path missing = tempDir.resolve("missing.acf");
        Optional<SteamAppManifest> result = SteamAppManifestParser.parse(missing);
        assertTrue(result.isEmpty());
    }
}
