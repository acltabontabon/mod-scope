package com.acltabontabon.modscope.core;

import java.nio.file.Path;

public record ScanOptions(
    String profileId,
    Path gameDir,
    Path outputDir,
    ScanMode mode
) {
    public static ScanOptions defaults() {
        return new ScanOptions(null, null, Path.of(".modscope", "reports"), ScanMode.STANDARD);
    }
}
