package com.acltabontabon.modscope.save;

import java.nio.file.Path;

public record SaveFileEntry(
    Path absolutePath,
    String displayName,
    long sizeBytes,
    String lastModified
) {}
