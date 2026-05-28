package com.acltabontabon.modscope.engine;

import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EngineDetectorTest {

    private static FileEntry entry(String path, String ext, FileCategory cat) {
        return new FileEntry(path, cat, ext, 1024, "", null, null);
    }

    @Test
    void unknownWhenNoSignals() {
        var result = EngineDetector.detect(List.of(
            entry("Retail/Game.exe", "exe", FileCategory.GAME_EXECUTABLE)
        ));
        assertFalse(result.isKnown());
        assertEquals(EngineFamily.UNKNOWN, result.primary());
    }

    @Test
    void detectsGlacierFromRpkg() {
        var result = EngineDetector.detect(List.of(
            entry("Runtime/chunk0.rpkg", "rpkg", FileCategory.ARCHIVE),
            entry("Runtime/chunk1.rpkg", "rpkg", FileCategory.ARCHIVE)
        ));
        assertTrue(result.isKnown());
        assertEquals(EngineFamily.GLACIER, result.primary());
        assertTrue(result.confidence() >= 90);
    }

    @Test
    void detectsUnrealFromIoStoreTrifecta() {
        var result = EngineDetector.detect(List.of(
            entry("Content/Paks/game.pak", "pak", FileCategory.ARCHIVE),
            entry("Content/Paks/game.ucas", "ucas", FileCategory.ARCHIVE),
            entry("Content/Paks/game.utoc", "utoc", FileCategory.ARCHIVE)
        ));
        assertTrue(result.isKnown());
        assertEquals(EngineFamily.UNREAL, result.primary());
        assertTrue(result.confidence() >= 95);
    }

    @Test
    void detectsUnrealFromPakOnly() {
        var result = EngineDetector.detect(List.of(
            entry("Content/Paks/game.pak", "pak", FileCategory.ARCHIVE)
        ));
        assertTrue(result.isKnown());
        assertEquals(EngineFamily.UNREAL, result.primary());
    }

    @Test
    void detectsCreationEngineFromBa2() {
        var result = EngineDetector.detect(List.of(
            entry("Data/Fallout4.ba2", "ba2", FileCategory.ARCHIVE)
        ));
        assertTrue(result.isKnown());
        assertEquals(EngineFamily.CREATION, result.primary());
    }

    @Test
    void detectsAnvilFromForge() {
        var result = EngineDetector.detect(List.of(
            entry("DataPC_pack_M.forge", "forge", FileCategory.ARCHIVE)
        ));
        assertTrue(result.isKnown());
        assertEquals(EngineFamily.ANVIL, result.primary());
    }

    @Test
    void glacierBeatsUnrealWhenRpkgOutweighs() {
        // Many rpkg files vs one pak
        var result = EngineDetector.detect(List.of(
            entry("chunk0.rpkg", "rpkg", FileCategory.ARCHIVE),
            entry("chunk1.rpkg", "rpkg", FileCategory.ARCHIVE),
            entry("chunk2.rpkg", "rpkg", FileCategory.ARCHIVE),
            entry("something.pak", "pak", FileCategory.ARCHIVE)
        ));
        assertEquals(EngineFamily.GLACIER, result.primary());
    }

    @Test
    void signalsListIsNotEmpty() {
        var result = EngineDetector.detect(List.of(
            entry("data.ba2", "ba2", FileCategory.ARCHIVE)
        ));
        assertFalse(result.signals().isEmpty());
        assertEquals(EngineFamily.CREATION, result.signals().get(0).family());
    }

    @Test
    void emptyFileListReturnsUnknown() {
        var result = EngineDetector.detect(List.of());
        assertEquals(EngineFamily.UNKNOWN, result.primary());
        assertEquals(0, result.confidence());
    }
}
