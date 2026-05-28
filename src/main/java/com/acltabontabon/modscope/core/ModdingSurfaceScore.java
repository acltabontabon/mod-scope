package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;

import java.util.List;

public enum ModdingSurfaceScore {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    public static ModdingSurfaceScore calculate(List<FileEntry> files, ScanResult result) {
        long total = files.size();
        if (total == 0) return NONE;

        long archives = files.stream().filter(f -> f.category() == FileCategory.ARCHIVE).count();
        long configs = files.stream().filter(f ->
            f.category() == FileCategory.CONFIG || f.category() == FileCategory.TEXT).count();

        double archiveRatio = (double) archives / total;
        int level = 0;

        // Archive-heavy game (>60% archives) — at least LOW
        if (archiveRatio > 0.6 && archives > 5) level = Math.max(level, 1);

        // Known engine with documented modding toolchain
        if (result.engineDetection().isKnown()) {
            EngineFamily family = result.engineDetection().primary();
            if (family == EngineFamily.UNREAL || family == EngineFamily.CREATION
                    || family == EngineFamily.GLACIER) {
                level = Math.max(level, 2);
            } else {
                level = Math.max(level, 1);
            }
        }

        // Loose config/text files are direct mod surface
        if (configs >= 10) level = Math.max(level, 2);
        if (configs >= 30) level = Math.max(level, 3);

        // QoL hints in readable text files
        if (!result.hints().isEmpty()) level = Math.max(level, 2);

        // PackageDefinition present = structured chunk manifest
        if (result.packageDefinition().found()) level = Math.max(level, 2);

        // Only use USEFUL binary hints (vendor noise excluded)
        int usefulBinary = result.binaryScan().usefulCount();
        if (usefulBinary >= 5) level = Math.max(level, 2);
        if (usefulBinary >= 20) level = Math.max(level, 3);

        return values()[level];
    }
}
