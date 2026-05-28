package com.acltabontabon.modscope.scan;

public record FileEntry(
    String relativePath,
    FileCategory category,
    String extension,
    long sizeBytes,
    String lastModified,
    String sha256,
    String hashSkippedReason
) {}
