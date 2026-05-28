package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.engine.EngineDetectionResult;
import com.acltabontabon.modscope.game.GameInstall;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.save.SaveFileEntry;
import com.acltabontabon.modscope.scan.BinaryStringHint;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.scan.HintMatch;
import com.acltabontabon.modscope.scan.PackageDefinitionAnalysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record ScanResult(
    Optional<GameInstall> install,
    List<SaveCandidate> saves,
    List<SaveFileEntry> saveInventory,
    List<FileEntry> files,
    List<HintMatch> hints,
    EngineDetectionResult engineDetection,
    PackageDefinitionAnalysis packageDefinition,
    List<BinaryStringHint> binaryHints,
    ModdingSurfaceScore surfaceScore,
    Path reportDir,
    String scannedAt,
    ScanOptions options
) {}
