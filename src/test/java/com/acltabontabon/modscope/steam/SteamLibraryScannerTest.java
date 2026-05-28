package com.acltabontabon.modscope.steam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SteamLibraryScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsRootLibraryWhenNoVdf() {
        List<Path> libraries = SteamLibraryScanner.findLibraryFolders(tempDir);
        assertEquals(1, libraries.size());
        assertEquals(tempDir, libraries.get(0));
    }

    @Test
    void findsAdditionalLibraryFromVdf() throws IOException {
        Path configDir = Files.createDirectories(tempDir.resolve("config"));
        Path extraLib = Files.createDirectories(tempDir.resolve("extra-library"));

        Path vdf = configDir.resolve("libraryfolders.vdf");
        Files.writeString(vdf, """
            "libraryfolders"
            {
            \t"0"
            \t{
            \t\t"path"\t\t"%s"
            \t}
            }
            """.formatted(extraLib.toString().replace("\\", "\\\\")), StandardCharsets.UTF_8);

        List<Path> libraries = SteamLibraryScanner.findLibraryFolders(tempDir);
        assertTrue(libraries.size() >= 2);
        assertTrue(libraries.contains(extraLib));
    }

    @Test
    void findsAppInstallDir() throws IOException {
        Path steamapps = Files.createDirectories(tempDir.resolve("steamapps"));
        Path commonDir = Files.createDirectories(steamapps.resolve("common").resolve("007 First Light"));

        Path acf = steamapps.resolve("appmanifest_3768760.acf");
        Files.writeString(acf, """
            "AppState"
            {
            \t"appid"\t\t"3768760"
            \t"name"\t\t"007 First Light"
            \t"installdir"\t\t"007 First Light"
            }
            """);

        Optional<Path> found = SteamLibraryScanner.findAppInstallDir(List.of(tempDir), "3768760");
        assertTrue(found.isPresent());
        assertEquals(commonDir, found.get());
    }
}
