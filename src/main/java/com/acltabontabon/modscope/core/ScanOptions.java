package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.scan.BinaryScanPolicy;
import com.acltabontabon.modscope.storage.AppPaths;

import java.nio.file.Path;

public record ScanOptions(
    String profileId,
    Path gameDir,
    Path outputDir,
    ScanMode mode,
    BinaryScanPolicy binaryScan
) {
    public static ScanOptions defaults() {
        return new ScanOptions(null, null, AppPaths.reportsRoot(),
            ScanMode.STANDARD, BinaryScanPolicy.conservative());
    }
}
