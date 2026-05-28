package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.scan.BinaryScanPolicy;

import java.nio.file.Path;

public record ScanOptions(
    String profileId,
    Path gameDir,
    Path outputDir,
    ScanMode mode,
    BinaryScanPolicy binaryScan
) {
    public static ScanOptions defaults() {
        return new ScanOptions(null, null, Path.of(".modscope", "reports"),
            ScanMode.STANDARD, BinaryScanPolicy.conservative());
    }
}
