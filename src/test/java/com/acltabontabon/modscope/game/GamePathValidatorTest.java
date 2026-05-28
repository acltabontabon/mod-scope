package com.acltabontabon.modscope.game;

import com.acltabontabon.modscope.game.profiles.FirstLightProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GamePathValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void failsForNonExistentPath() {
        Path missing = tempDir.resolve("does-not-exist");
        GamePathValidator.ValidationResult result = GamePathValidator.validate(missing, FirstLightProfile.INSTANCE);
        assertFalse(result.valid());
    }

    @Test
    void failsWhenNoExpectedStructureFound() {
        GamePathValidator.ValidationResult result = GamePathValidator.validate(tempDir, FirstLightProfile.INSTANCE);
        assertFalse(result.valid());
    }

    @Test
    void succeedsWhenExpectedSubfolderExists() throws IOException {
        Files.createDirectories(tempDir.resolve("Retail"));

        GamePathValidator.ValidationResult result = GamePathValidator.validate(tempDir, FirstLightProfile.INSTANCE);
        assertTrue(result.valid());
        assertFalse(result.subfolders().isEmpty());
    }

    @Test
    void succeedsWhenExpectedExecutableExists() throws IOException {
        Path retailDir = Files.createDirectories(tempDir.resolve("Retail"));
        Files.createFile(retailDir.resolve("Game.exe"));

        GamePathValidator.ValidationResult result = GamePathValidator.validate(tempDir, FirstLightProfile.INSTANCE);
        assertTrue(result.valid());
        assertFalse(result.executables().isEmpty());
    }
}
