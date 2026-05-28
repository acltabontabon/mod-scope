package com.acltabontabon.modscope.settings;

import com.acltabontabon.modscope.core.ScanMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppSettingsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileReturnsDefaults() {
        var store = new AppSettingsStore(tempDir.resolve("settings.json"));
        var settings = store.load();
        assertEquals(ScanMode.STANDARD, settings.defaultScanMode());
        assertFalse(settings.includeBinaryStringScanByDefault());
    }

    @Test
    void roundTripsCustomValues() throws IOException {
        Path file = tempDir.resolve("settings.json");
        var store = new AppSettingsStore(file);
        var original = new AppSettings(tempDir, ScanMode.DEEP, true, true, false, true, 16, 200, false);
        store.save(original);

        var loaded = store.load();
        assertEquals(ScanMode.DEEP, loaded.defaultScanMode());
        assertTrue(loaded.includeBinaryStringScanByDefault());
        assertTrue(loaded.includeVendorRuntimeLibs());
        assertFalse(loaded.includeGameExecutableStrings());
        assertTrue(loaded.includeLargeArchiveSampling());
        assertEquals(16, loaded.maxStringSampleMb());
        assertEquals(200, loaded.maxHashSizeMb());
        assertFalse(loaded.showAdvancedWarnings());
    }

    @Test
    void corruptFileFallsBackToDefaults() throws IOException {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "not json");
        var store = new AppSettingsStore(file);
        assertEquals(ScanMode.STANDARD, store.load().defaultScanMode());
    }

    @Test
    void partialFileMergesWithDefaults() throws IOException {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "{ \"defaultScanMode\": \"QUICK\" }");
        var store = new AppSettingsStore(file);
        var s = store.load();
        assertEquals(ScanMode.QUICK, s.defaultScanMode());
        // Other fields fall back to defaults.
        assertFalse(s.includeBinaryStringScanByDefault());
        assertEquals(100, s.maxHashSizeMb());
    }
}
