package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.game.GameInstall;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.scan.HintMatch;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record ScanResult(
    Optional<GameInstall> install,
    List<SaveCandidate> saves,
    List<FileEntry> files,
    List<HintMatch> hints,
    Path reportDir,
    String scannedAt,
    ScanOptions options
) {}
