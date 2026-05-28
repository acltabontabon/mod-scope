package com.acltabontabon.modscope.inference;

import com.acltabontabon.modscope.engine.EngineDetectionResult;
import com.acltabontabon.modscope.engine.EngineDetector;
import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.engine.EngineSignal;
import com.acltabontabon.modscope.scan.FileEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps {@link EngineDetector} with additional engine-family heuristics that the
 * core detector doesn't cover (Unity, REDengine, Source). When no engine matches,
 * produces a generic-layout classification so the triage screen always has something
 * concrete to say.
 *
 * Signals are advisory — each carries its own confidence weight; the caller should
 * never treat any single signal as conclusive.
 */
public final class GenericGameProfileInferer {

    private GenericGameProfileInferer() {}

    public static EngineDetectionResult infer(List<FileEntry> files) {
        EngineDetectionResult base = EngineDetector.detect(files);
        List<EngineSignal> extra = new ArrayList<>();

        boolean hasUnityPlayer = false;
        boolean hasGameAssembly = false;
        boolean hasGlobalMetadata = false;
        boolean hasUnityDataDir = false;
        boolean hasArchive = false;            // REDengine
        boolean hasR6Config = false;
        boolean hasArchivePcContent = false;
        boolean hasHl2Exe = false;
        boolean hasGameInfoTxt = false;
        boolean hasVpk = false;
        boolean hasPackageDefinitionTxt = false;
        boolean hasUnrealPaksDir = false;
        boolean hasEngineBinariesDir = false;
        boolean hasBethesdaDataDir = false;

        for (FileEntry entry : files) {
            String rel = entry.relativePath().toLowerCase();
            String filename = filename(rel);
            String ext = entry.extension().toLowerCase();

            switch (filename) {
                case "unityplayer.dll" -> hasUnityPlayer = true;
                case "gameassembly.dll" -> hasGameAssembly = true;
                case "global-metadata.dat" -> hasGlobalMetadata = true;
                case "hl2.exe" -> hasHl2Exe = true;
                case "gameinfo.txt" -> hasGameInfoTxt = true;
                case "packagedefinition.txt" -> hasPackageDefinitionTxt = true;
                default -> {}
            }
            if (rel.endsWith("_data/") || rel.contains("_data/")) hasUnityDataDir = true;
            if (ext.equals("archive")) hasArchive = true;
            if (rel.startsWith("r6/config/") || rel.contains("/r6/config/")) hasR6Config = true;
            if (rel.startsWith("archive/pc/content/") || rel.contains("/archive/pc/content/")) hasArchivePcContent = true;
            if (ext.equals("vpk")) hasVpk = true;
            if (rel.contains("/content/paks/") || rel.startsWith("content/paks/")) hasUnrealPaksDir = true;
            if (rel.contains("engine/binaries/") || rel.startsWith("engine/binaries/")) hasEngineBinariesDir = true;
            if (rel.startsWith("data/") && (ext.equals("ba2") || ext.equals("bsa") || ext.equals("esp") || ext.equals("esm"))) {
                hasBethesdaDataDir = true;
            }
        }

        // Unity
        if (hasUnityPlayer) extra.add(new EngineSignal(EngineFamily.UNITY, "UnityPlayer.dll", 95));
        if (hasGameAssembly) extra.add(new EngineSignal(EngineFamily.UNITY, "GameAssembly.dll (IL2CPP)", 70));
        if (hasGlobalMetadata) extra.add(new EngineSignal(EngineFamily.UNITY, "global-metadata.dat", 70));
        if (hasUnityDataDir) extra.add(new EngineSignal(EngineFamily.UNITY, "<Game>_Data/ directory", 60));

        // REDengine
        if (hasArchive && hasR6Config) {
            extra.add(new EngineSignal(EngineFamily.REDENGINE, ".archive + r6/config (Cyberpunk 2077 / REDengine 4 layout)", 95));
        } else if (hasArchive) {
            extra.add(new EngineSignal(EngineFamily.REDENGINE, ".archive files", 60));
        }
        if (hasArchivePcContent) extra.add(new EngineSignal(EngineFamily.REDENGINE, "archive/pc/content/", 70));

        // Source
        if (hasHl2Exe) extra.add(new EngineSignal(EngineFamily.SOURCE, "hl2.exe", 90));
        if (hasGameInfoTxt) extra.add(new EngineSignal(EngineFamily.SOURCE, "gameinfo.txt", 80));
        if (hasVpk) extra.add(new EngineSignal(EngineFamily.SOURCE, ".vpk files", 70));

        // Glacier reinforcement
        if (hasPackageDefinitionTxt) {
            extra.add(new EngineSignal(EngineFamily.GLACIER, "packagedefinition.txt", 85));
        }

        // Unreal reinforcement
        if (hasUnrealPaksDir) extra.add(new EngineSignal(EngineFamily.UNREAL, "<Game>/Content/Paks/ directory", 75));
        if (hasEngineBinariesDir) extra.add(new EngineSignal(EngineFamily.UNREAL, "Engine/Binaries/ directory", 70));

        // Bethesda reinforcement
        if (hasBethesdaDataDir) extra.add(new EngineSignal(EngineFamily.CREATION, "Data/ directory with .ba2/.bsa/.esp/.esm", 80));

        if (extra.isEmpty()) return base;

        // Merge base + extra and recompute primary/confidence.
        List<EngineSignal> all = new ArrayList<>(base.signals());
        all.addAll(extra);

        Map<EngineFamily, Integer> totals = new HashMap<>();
        for (EngineSignal s : all) totals.merge(s.family(), s.weight(), Integer::sum);

        EngineFamily primary = totals.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(EngineFamily.UNKNOWN);

        int confidence = Math.min(100, totals.getOrDefault(primary, 0));
        return new EngineDetectionResult(primary, confidence, List.copyOf(all));
    }

    private static String filename(String rel) {
        int sep = rel.lastIndexOf('/');
        return sep >= 0 ? rel.substring(sep + 1) : rel;
    }
}
