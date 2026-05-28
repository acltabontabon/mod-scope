package com.acltabontabon.modscope.scan;

public record BinaryStringHint(
    String relativePath,
    long fileOffset,
    String keyword,
    String context
) {}
