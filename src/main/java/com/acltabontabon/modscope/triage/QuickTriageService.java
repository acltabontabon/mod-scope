package com.acltabontabon.modscope.triage;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.engine.EngineDetectionResult;
import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.game.GameProfile;
import com.acltabontabon.modscope.game.GameProfileRegistry;
import com.acltabontabon.modscope.inference.GenericGameProfileInferer;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.recommendation.Recommendation;
import com.acltabontabon.modscope.recommendation.RecommendationEngine;
import com.acltabontabon.modscope.recommendation.RecommendationType;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.save.SaveLocator;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.scan.FileInventoryScanner;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/**
 * Fast triage scan. No hashing, no binary string scan, no text-hint deep read.
 * Produces a {@link GameTriageResult} with surface levels, badges, and a recommended next action.
 */
@Service
public class QuickTriageService {

    /** Hard cap so a single misconfigured directory cannot freeze the dashboard. */
    private static final int MAX_FILES_INSPECTED = 50_000;
    private static final long MAX_BYTES_INSPECTED = 5L * 1024L * 1024L * 1024L; // 5 GB cap

    public TriageOutcome triage(DetectedGame game) {
        return triage(game, null);
    }

    public TriageOutcome triage(DetectedGame game, Consumer<String> log) {
        Consumer<String> sink = log != null ? log : s -> {};
        if (game.installPath() == null || !Files.isDirectory(game.installPath())) {
            return TriageOutcome.failed(game, "Install path not found: " + game.installPath());
        }

        sink.accept("Walking " + game.installPath());
        List<FileEntry> files = new ArrayList<>();
        int[] count = {0};
        long[] bytes = {0L};
        boolean[] truncated = {false};

        FileInventoryScanner.scan(game.installPath(), ScanMode.QUICK, entry -> {
            if (truncated[0]) return;
            files.add(entry);
            count[0]++;
            bytes[0] += Math.max(0, entry.sizeBytes());
            if (count[0] >= MAX_FILES_INSPECTED || bytes[0] >= MAX_BYTES_INSPECTED) {
                truncated[0] = true;
            }
        });
        sink.accept("Inspected " + files.size() + " files" + (truncated[0] ? " (truncated)" : ""));

        EngineDetectionResult engine = GenericGameProfileInferer.infer(files);

        long total = Math.max(1, files.size());
        long configs = files.stream()
            .filter(f -> f.category() == FileCategory.CONFIG || f.category() == FileCategory.TEXT)
            .count();
        long archives = files.stream()
            .filter(f -> f.category() == FileCategory.ARCHIVE)
            .count();
        long vendor = files.stream()
            .filter(f -> isVendor(f.category()))
            .count();
        long otherCount = total - vendor - archives;

        SurfaceLevel configSurface = scale(configs);
        SurfaceLevel packedSurface = scaleArchives(archives, total);
        SurfaceLevel looseSurface = scaleLoose(otherCount, total, vendor);

        // Save surface: try the appId-matched profile, otherwise NONE.
        SurfaceLevel saveSurface = SurfaceLevel.NONE;
        if (game.appId() != null) {
            var profile = GameProfileRegistry.findByAppId(game.appId()).orElse(null);
            if (profile != null) {
                List<SaveCandidate> saves = SaveLocator.locate(profile);
                long present = saves.stream().filter(s -> s.exists() && s.sizeBytes() > 0).count();
                if (present > 0) saveSurface = present > 1 ? SurfaceLevel.MEDIUM : SurfaceLevel.LOW;
            }
        }

        ExternalToolDependency external = externalToolDep(engine.primary(), packedSurface, looseSurface);
        int scanConfidence = computeConfidence(engine, files.size(), truncated[0]);
        List<Badge> badges = BadgeCalculator.compute(engine, configSurface, saveSurface, looseSurface, packedSurface, external, scanConfidence);

        Set<String> archiveFormats = files.stream()
            .filter(f -> f.category() == FileCategory.ARCHIVE)
            .map(FileEntry::extension)
            .filter(e -> !e.isBlank())
            .map(e -> "." + e.toLowerCase())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        GameTriageResult prelim = new GameTriageResult(
            game.id(),
            game.displayName(),
            game.installPath(),
            game.source(),
            engine.primary(),
            engine.confidence(),
            List.copyOf(archiveFormats),
            configSurface,
            saveSurface,
            looseSurface,
            packedSurface,
            external,
            RecommendationType.MANUAL_REVIEW,
            scanConfidence,
            Instant.now(),
            badges,
            files.size(),
            bytes[0],
            engineRationale(engine)
        );

        List<Recommendation> recs = RecommendationEngine.evaluate(prelim);
        RecommendationType top = recs.isEmpty() ? RecommendationType.MANUAL_REVIEW : recs.get(0).type();

        GameTriageResult finalResult = new GameTriageResult(
            prelim.gameId(), prelim.gameName(), prelim.installPath(), prelim.source(),
            prelim.detectedEngine(), prelim.engineConfidence(), prelim.archiveFormats(),
            prelim.configSurface(), prelim.saveSurface(), prelim.looseFileSurface(),
            prelim.packedArchiveSurface(), prelim.externalToolDependency(),
            top,
            prelim.scanConfidence(), prelim.lastScannedAt(), prelim.badges(),
            prelim.filesScanned(), prelim.bytesScanned(), prelim.engineRationale()
        );

        sink.accept("Engine: " + engine.primary() + " (" + engine.confidence() + "%)");
        sink.accept("Recommended: " + top);

        return TriageOutcome.success(finalResult, recs);
    }

    private static boolean isVendor(FileCategory c) {
        return c == FileCategory.VENDOR_LIBRARY
            || c == FileCategory.NVIDIA_LIBRARY
            || c == FileCategory.STEAM_LIBRARY
            || c == FileCategory.GRAPHICS_LIBRARY
            || c == FileCategory.STREAMLINE_LIBRARY
            || c == FileCategory.PHYSX_LIBRARY
            || c == FileCategory.DIRECTSTORAGE_LIBRARY
            || c == FileCategory.SYSTEM_COMPAT_LIBRARY
            || c == FileCategory.RUNTIME_LIBRARY;
    }

    private static SurfaceLevel scale(long count) {
        if (count <= 0) return SurfaceLevel.NONE;
        if (count < 10) return SurfaceLevel.LOW;
        if (count < 50) return SurfaceLevel.MEDIUM;
        return SurfaceLevel.HIGH;
    }

    private static SurfaceLevel scaleArchives(long archives, long total) {
        if (archives <= 0) return SurfaceLevel.NONE;
        double ratio = (double) archives / total;
        if (archives >= 50 || ratio > 0.4) return SurfaceLevel.HIGH;
        if (archives >= 10 || ratio > 0.15) return SurfaceLevel.MEDIUM;
        return SurfaceLevel.LOW;
    }

    private static SurfaceLevel scaleLoose(long loose, long total, long vendor) {
        if (loose <= 0) return SurfaceLevel.NONE;
        double vendorRatio = (double) vendor / total;
        SurfaceLevel raw = scale(loose);
        // Heavy vendor noise drags the perceived loose surface down by one level.
        if (vendorRatio > 0.5 && raw != SurfaceLevel.NONE) {
            return SurfaceLevel.values()[Math.max(0, raw.ordinal() - 1)];
        }
        return raw;
    }

    private static ExternalToolDependency externalToolDep(EngineFamily family, SurfaceLevel archives, SurfaceLevel loose) {
        if (archives == SurfaceLevel.HIGH && loose.ordinal() <= SurfaceLevel.LOW.ordinal()) {
            return switch (family) {
                case GLACIER, UNREAL, CREATION, REDENGINE, ANVIL, UNITY -> ExternalToolDependency.REQUIRED;
                default -> ExternalToolDependency.LIKELY;
            };
        }
        if (archives.atLeast(SurfaceLevel.MEDIUM)) return ExternalToolDependency.LIKELY;
        return ExternalToolDependency.NONE;
    }

    private static int computeConfidence(EngineDetectionResult engine, int fileCount, boolean truncated) {
        int base = engine.primary() == EngineFamily.UNKNOWN ? 40 : Math.min(90, 50 + engine.confidence() / 4);
        if (fileCount < 20) base -= 20;
        if (truncated) base -= 10;
        return Math.max(0, Math.min(100, base));
    }

    private static String engineRationale(EngineDetectionResult engine) {
        if (engine.signals().isEmpty()) return "No engine signals detected.";
        return engine.signals().stream()
            .limit(3)
            .map(s -> s.evidence() + " (" + s.weight() + "%)")
            .collect(java.util.stream.Collectors.joining("; "));
    }

    public record TriageOutcome(
        DetectedGame game,
        GameTriageResult result,
        List<Recommendation> recommendations,
        boolean success,
        String error
    ) {
        public static TriageOutcome success(GameTriageResult r, List<Recommendation> recs) {
            return new TriageOutcome(null, r, recs, true, null);
        }
        public static TriageOutcome failed(DetectedGame game, String err) {
            return new TriageOutcome(game, null, List.of(), false, err);
        }
    }
}
