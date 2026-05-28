package com.acltabontabon.modscope.game;

import java.nio.file.Path;
import java.util.List;

public record GameInstall(
    GameProfile profile,
    Path installPath,
    List<Path> detectedSubfolders,
    List<Path> detectedExecutables
) {}
