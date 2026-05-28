package com.acltabontabon.modscope.settings;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.storage.AppPaths;

import java.nio.file.Path;

public record AppSettings(
    Path reportsDir,
    ScanMode defaultScanMode,
    boolean includeBinaryStringScanByDefault,
    boolean includeVendorRuntimeLibs,
    boolean includeGameExecutableStrings,
    boolean includeLargeArchiveSampling,
    int maxStringSampleMb,
    int maxHashSizeMb,
    boolean showAdvancedWarnings
) {
    public static AppSettings defaults() {
        return new AppSettings(
            AppPaths.reportsRoot(),
            ScanMode.STANDARD,
            false,
            false,
            false,
            false,
            8,
            100,
            true
        );
    }
}
