package com.acltabontabon.modscope.save;

import java.nio.file.Path;

public record SaveCandidate(
    Path path,
    boolean exists,
    long sizeBytes,
    String lastModified
) {}
