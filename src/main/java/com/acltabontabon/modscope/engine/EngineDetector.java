package com.acltabontabon.modscope.engine;

import com.acltabontabon.modscope.scan.FileEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EngineDetector {

    private EngineDetector() {}

    public static EngineDetectionResult detect(List<FileEntry> files) {
        List<EngineSignal> signals = new ArrayList<>();

        boolean hasPak = false, hasUcas = false, hasUtoc = false;
        boolean hasRpkg = false;
        boolean hasBsa = false, hasBa2 = false;
        boolean hasForge = false;
        boolean hasBik2 = false;
        boolean hasAssetRegistry = false;
        boolean hasGlacierExe = false;
        boolean hasUE4SS = false;

        for (FileEntry entry : files) {
            String ext = entry.extension().toLowerCase();
            String name = entry.relativePath().toLowerCase();
            String filename = filename(entry.relativePath()).toLowerCase();

            switch (ext) {
                case "pak"  -> hasPak = true;
                case "ucas" -> hasUcas = true;
                case "utoc" -> hasUtoc = true;
                case "rpkg" -> hasRpkg = true;
                case "bsa"  -> hasBsa = true;
                case "ba2"  -> hasBa2 = true;
                case "forge" -> hasForge = true;
                case "bik2" -> hasBik2 = true;
            }

            if (filename.equals("assetregistry.bin")) hasAssetRegistry = true;
            if (filename.startsWith("glacier2") || filename.startsWith("hitmanbloodmoney")
                    || filename.contains("glacier")) hasGlacierExe = true;
            if (filename.startsWith("ue4ss") || filename.startsWith("ue5ss")
                    || name.contains("ue4ss") || name.contains("ue5ss")) hasUE4SS = true;
        }

        // Glacier signals
        if (hasRpkg) {
            signals.add(new EngineSignal(EngineFamily.GLACIER, ".rpkg package files", 90));
        }
        if (hasBik2) {
            signals.add(new EngineSignal(EngineFamily.GLACIER, ".bik2 video files (IO Interactive format)", 60));
        }
        if (hasGlacierExe) {
            signals.add(new EngineSignal(EngineFamily.GLACIER, "glacier2 executable detected", 80));
        }

        // Unreal signals
        if (hasPak && hasUcas && hasUtoc) {
            signals.add(new EngineSignal(EngineFamily.UNREAL, ".pak + .ucas + .utoc (Unreal Engine 5 IoStore)", 95));
        } else if (hasPak && (hasUcas || hasUtoc)) {
            signals.add(new EngineSignal(EngineFamily.UNREAL, ".pak + IoStore files", 80));
        } else if (hasPak) {
            signals.add(new EngineSignal(EngineFamily.UNREAL, ".pak archive files", 50));
        }
        if (hasAssetRegistry) {
            signals.add(new EngineSignal(EngineFamily.UNREAL, "AssetRegistry.bin (Unreal content registry)", 85));
        }
        if (hasUE4SS) {
            signals.add(new EngineSignal(EngineFamily.UNREAL, "UE4SS/UE5SS mod loader detected", 95));
        }

        // Creation Engine signals
        if (hasBa2) {
            signals.add(new EngineSignal(EngineFamily.CREATION, ".ba2 archive files (Creation Engine)", 90));
        }
        if (hasBsa) {
            signals.add(new EngineSignal(EngineFamily.CREATION, ".bsa archive files (Bethesda/Creation)", 85));
        }

        // Anvil signals
        if (hasForge) {
            signals.add(new EngineSignal(EngineFamily.ANVIL, ".forge archive files (Ubisoft AnvilNext)", 90));
        }

        if (signals.isEmpty()) return EngineDetectionResult.unknown();

        // Aggregate weights per family
        Map<EngineFamily, Integer> totals = new HashMap<>();
        for (EngineSignal s : signals) {
            totals.merge(s.family(), s.weight(), Integer::sum);
        }

        EngineFamily primary = totals.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(EngineFamily.UNKNOWN);

        int confidence = Math.min(100, totals.getOrDefault(primary, 0));

        return new EngineDetectionResult(primary, confidence, List.copyOf(signals));
    }

    private static String filename(String relativePath) {
        int sep = Math.max(relativePath.lastIndexOf('/'), relativePath.lastIndexOf('\\'));
        return sep >= 0 ? relativePath.substring(sep + 1) : relativePath;
    }
}
