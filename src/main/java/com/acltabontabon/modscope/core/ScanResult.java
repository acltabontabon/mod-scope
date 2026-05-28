package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.engine.EngineDetectionResult;
import com.acltabontabon.modscope.game.GameInstall;
import com.acltabontabon.modscope.recommendation.Recommendation;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.save.SaveFileEntry;
import com.acltabontabon.modscope.scan.BinaryScanResult;
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
    BinaryScanResult binaryScan,
    ModdingSurfaceScore surfaceScore,
    List<Recommendation> recommendations,
    Path reportDir,
    String scannedAt,
    ScanOptions options
) {}
