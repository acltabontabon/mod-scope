package com.acltabontabon.modscope.inference;

import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericGameProfileInfererTest {

    @Test
    void detectsUnrealFromPakAndIoStore() {
        var files = List.of(
            entry("Game/Content/Paks/Game-WindowsClient.pak", "pak", FileCategory.ARCHIVE),
            entry("Game/Content/Paks/global.ucas", "ucas", FileCategory.ARCHIVE),
            entry("Game/Content/Paks/global.utoc", "utoc", FileCategory.ARCHIVE)
        );
        assertEquals(EngineFamily.UNREAL, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void detectsUnityFromUnityPlayer() {
        var files = List.of(entry("UnityPlayer.dll", "dll", FileCategory.VENDOR_LIBRARY));
        assertEquals(EngineFamily.UNITY, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void detectsGlacierFromRpkgAndPackageDefinition() {
        var files = List.of(
            entry("Runtime/chunk0.rpkg", "rpkg", FileCategory.ARCHIVE),
            entry("Runtime/packagedefinition.txt", "txt", FileCategory.PACKAGE_DEFINITION)
        );
        assertEquals(EngineFamily.GLACIER, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void detectsRedengineFromArchiveAndR6Config() {
        var files = List.of(
            entry("archive/pc/content/basegame_4_gamedata.archive", "archive", FileCategory.ARCHIVE),
            entry("r6/config/settings.json", "json", FileCategory.CONFIG)
        );
        assertEquals(EngineFamily.REDENGINE, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void detectsBethesdaFromBa2() {
        var files = List.of(entry("Data/Fallout4 - Animations.ba2", "ba2", FileCategory.ARCHIVE));
        assertEquals(EngineFamily.CREATION, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void detectsSourceFromHl2Exe() {
        var files = List.of(
            entry("hl2.exe", "exe", FileCategory.GAME_EXECUTABLE),
            entry("hl2/gameinfo.txt", "txt", FileCategory.TEXT)
        );
        assertEquals(EngineFamily.SOURCE, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void unknownLayoutReturnsUnknown() {
        var files = List.of(entry("readme.txt", "txt", FileCategory.TEXT));
        assertEquals(EngineFamily.UNKNOWN, GenericGameProfileInferer.infer(files).primary());
    }

    @Test
    void higherWeightWinsWhenSignalsConflict() {
        var files = List.of(
            entry("UnityPlayer.dll", "dll", FileCategory.VENDOR_LIBRARY),
            entry("Game.pak", "pak", FileCategory.ARCHIVE) // weak Unreal signal
        );
        // Unity has weight 95, Unreal .pak alone has weight 50
        assertEquals(EngineFamily.UNITY, GenericGameProfileInferer.infer(files).primary());
        assertTrue(GenericGameProfileInferer.infer(files).confidence() >= 90);
    }

    private static FileEntry entry(String path, String ext, FileCategory cat) {
        return new FileEntry(path, cat, ext, 1024, "2026-01-01T00:00:00Z", null, null);
    }
}
