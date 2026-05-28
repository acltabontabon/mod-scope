package com.acltabontabon.modscope.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileClassifierTest {

    @Test
    void classifiesGameExecutable() {
        assertEquals(FileCategory.GAME_EXECUTABLE, FileClassifier.classify("Game.exe", "exe", 1024));
        assertEquals(FileCategory.GAME_EXECUTABLE, FileClassifier.classify("app.bin", "bin", 512 * 1024));
    }

    @Test
    void classifiesRuntimeLibrary() {
        assertEquals(FileCategory.RUNTIME_LIBRARY, FileClassifier.classify("something.dll", "dll", 512 * 1024));
        assertEquals(FileCategory.RUNTIME_LIBRARY, FileClassifier.classify("libsomething.so", "so", 1024));
    }

    @Test
    void classifiesGraphicsLibrary() {
        assertEquals(FileCategory.GRAPHICS_LIBRARY, FileClassifier.classify("D3D12Core.dll", "dll", 1024));
        assertEquals(FileCategory.GRAPHICS_LIBRARY, FileClassifier.classify("dxgi.dll", "dll", 1024));
        assertEquals(FileCategory.GRAPHICS_LIBRARY, FileClassifier.classify("vulkan-1.dll", "dll", 1024));
        assertEquals(FileCategory.GRAPHICS_LIBRARY, FileClassifier.classify("d3dcompiler_47.dll", "dll", 1024));
    }

    @Test
    void classifiesStreamlineLibrary() {
        assertEquals(FileCategory.STREAMLINE_LIBRARY, FileClassifier.classify("sl.dlss.dll", "dll", 1024));
        assertEquals(FileCategory.STREAMLINE_LIBRARY, FileClassifier.classify("sl.interposer.dll", "dll", 1024));
    }

    @Test
    void classifiesPhysxLibrary() {
        assertEquals(FileCategory.PHYSX_LIBRARY, FileClassifier.classify("PhysX_64.dll", "dll", 1024));
        assertEquals(FileCategory.PHYSX_LIBRARY, FileClassifier.classify("nvPhysX.dll", "dll", 1024));
        assertEquals(FileCategory.PHYSX_LIBRARY, FileClassifier.classify("PxFoundation_x64.dll", "dll", 1024));
    }

    @Test
    void classifiesDirectStorageLibrary() {
        assertEquals(FileCategory.DIRECTSTORAGE_LIBRARY, FileClassifier.classify("dstorage.dll", "dll", 1024));
        assertEquals(FileCategory.DIRECTSTORAGE_LIBRARY, FileClassifier.classify("dstoragecore.dll", "dll", 1024));
    }

    @Test
    void classifiesSystemCompatLibrary() {
        assertEquals(FileCategory.SYSTEM_COMPAT_LIBRARY, FileClassifier.classify("vcruntime140.dll", "dll", 1024));
        assertEquals(FileCategory.SYSTEM_COMPAT_LIBRARY, FileClassifier.classify("msvcp140.dll", "dll", 1024));
        assertEquals(FileCategory.SYSTEM_COMPAT_LIBRARY, FileClassifier.classify("ucrtbase.dll", "dll", 1024));
    }

    @Test
    void isVendorLibraryReturnsTrueForVendorCategories() {
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.RUNTIME_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.NVIDIA_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.STEAM_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.GRAPHICS_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.STREAMLINE_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.PHYSX_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.DIRECTSTORAGE_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.SYSTEM_COMPAT_LIBRARY));
        assertTrue(FileClassifier.isVendorLibrary(FileCategory.VENDOR_LIBRARY));
        assertFalse(FileClassifier.isVendorLibrary(FileCategory.ARCHIVE));
        assertFalse(FileClassifier.isVendorLibrary(FileCategory.GAME_EXECUTABLE));
        assertFalse(FileClassifier.isVendorLibrary(FileCategory.CONFIG));
    }

    @Test
    void classifiesNvidiaLibrary() {
        assertEquals(FileCategory.NVIDIA_LIBRARY, FileClassifier.classify("nvapi64.dll", "dll", 1024));
        assertEquals(FileCategory.NVIDIA_LIBRARY, FileClassifier.classify("cudart64_110.dll", "dll", 1024));
    }

    @Test
    void classifiesSteamLibrary() {
        assertEquals(FileCategory.STEAM_LIBRARY, FileClassifier.classify("steam_api64.dll", "dll", 1024));
        assertEquals(FileCategory.STEAM_LIBRARY, FileClassifier.classify("steam_api.dll", "dll", 1024));
    }

    @Test
    void classifiesArchive() {
        assertEquals(FileCategory.ARCHIVE, FileClassifier.classify("data.pak", "pak", 100 * 1024 * 1024));
        assertEquals(FileCategory.ARCHIVE, FileClassifier.classify("data.rpkg", "rpkg", 50 * 1024 * 1024));
        assertEquals(FileCategory.ARCHIVE, FileClassifier.classify("chunk0.utoc", "utoc", 10 * 1024 * 1024));
    }

    @Test
    void classifiesPackageDefinition() {
        assertEquals(FileCategory.PACKAGE_DEFINITION,
            FileClassifier.classify("packagedefinition.txt", "txt", 1024));
        // only exact filename match; other .txt files stay TEXT
        assertEquals(FileCategory.TEXT,
            FileClassifier.classify("readme.txt", "txt", 1024));
    }

    @Test
    void classifiesConfig() {
        assertEquals(FileCategory.CONFIG, FileClassifier.classify("settings.ini", "ini", 512));
        assertEquals(FileCategory.CONFIG, FileClassifier.classify("game.cfg", "cfg", 1024));
    }

    @Test
    void classifiesVideo() {
        assertEquals(FileCategory.VIDEO, FileClassifier.classify("intro.bik", "bik", 50 * 1024 * 1024));
        assertEquals(FileCategory.VIDEO, FileClassifier.classify("cutscene.bik2", "bik2", 1024));
    }

    @Test
    void classifiesText() {
        assertEquals(FileCategory.TEXT, FileClassifier.classify("notes.json", "json", 1024));
        assertEquals(FileCategory.TEXT, FileClassifier.classify("readme.xml", "xml", 1024));
    }

    @Test
    void classifiesUnknownLargeForHugeFiles() {
        long huge = 600L * 1024 * 1024;
        assertEquals(FileCategory.UNKNOWN_LARGE, FileClassifier.classify("data.dat2", "dat2", huge));
    }

    @Test
    void classifiesOtherForSmallUnknownFiles() {
        assertEquals(FileCategory.OTHER, FileClassifier.classify("mystery.xyz", "xyz", 1024));
    }

    @Test
    void isTextReadableForKnownExtensions() {
        assertTrue(FileClassifier.isTextReadable("cfg"));
        assertTrue(FileClassifier.isTextReadable("json"));
        assertTrue(FileClassifier.isTextReadable("ini"));
        assertFalse(FileClassifier.isTextReadable("pak"));
        assertFalse(FileClassifier.isTextReadable("exe"));
    }

}
