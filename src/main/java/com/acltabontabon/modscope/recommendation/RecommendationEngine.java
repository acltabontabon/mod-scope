package com.acltabontabon.modscope.recommendation;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.engine.EngineFamily;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.triage.Badge;
import com.acltabontabon.modscope.triage.ExternalToolDependency;
import com.acltabontabon.modscope.triage.GameTriageResult;
import com.acltabontabon.modscope.triage.SurfaceLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure rule engine that ranks {@link Recommendation}s for either a quick triage or a full scan.
 * Wording in {@code reason} and {@code suggestedAction} is tuned for the plain-English style
 * required by the project spec.
 */
public final class RecommendationEngine {

    private RecommendationEngine() {}

    public static List<Recommendation> evaluate(GameTriageResult triage) {
        List<Recommendation> out = new ArrayList<>();

        boolean knownEngine = triage.detectedEngine() != EngineFamily.UNKNOWN;
        boolean configRich = triage.configSurface().atLeast(SurfaceLevel.MEDIUM);
        boolean archiveHeavy = triage.packedArchiveSurface().atLeast(SurfaceLevel.HIGH);
        boolean savesExist = triage.saveSurface().atLeast(SurfaceLevel.LOW);
        boolean looseHeavy = triage.looseFileSurface().atLeast(SurfaceLevel.MEDIUM);

        if (configRich && knownEngine && looseHeavy) {
            out.add(new Recommendation(
                RecommendationType.GOOD_FIRST_MOD_TARGET,
                "Good first mod target",
                "This game has loose config files, a known " + triage.detectedEngine() + " layout, and readable text content. " +
                "Simple ini/json/text tweaks are realistic without external tooling.",
                85,
                "Start with config edits. Back up the original file, change one value, and re-launch.",
                RiskLevel.LOW,
                List.of()
            ));
            out.add(new Recommendation(
                RecommendationType.INSPECT_CONFIGS,
                "Inspect loose config files",
                "Loose config files are present and readable as text.",
                80,
                "Open the candidates report and look for *.ini, *.cfg, *.json, *.xml under the install root.",
                RiskLevel.LOW,
                List.of()
            ));
        } else if (configRich) {
            out.add(new Recommendation(
                RecommendationType.INSPECT_CONFIGS,
                "Inspect loose config files",
                "Loose configuration files were detected. Simple ini/json/text tweaks may be possible from the install folder.",
                70,
                "Review the candidates and text-hints reports for promising keys to tweak.",
                RiskLevel.LOW,
                List.of()
            ));
        }

        if (archiveHeavy) {
            String toolName = externalToolName(triage.detectedEngine());
            out.add(new Recommendation(
                RecommendationType.INSPECT_ARCHIVES,
                "Archive listing recommended",
                "Most game content appears to be packed into large package files. Loose-file QoL mods may be limited. " +
                "Use read-only listing first. Do not extract, rebuild, or replace archives without backups and a clear modding workflow.",
                80,
                "List archive contents read-only before considering extraction.",
                RiskLevel.LOW,
                List.of()
            ));
            if (toolName != null) {
                out.add(new Recommendation(
                    RecommendationType.SET_UP_EXTERNAL_TOOL,
                    "External tool likely required",
                    "Asset modding for " + triage.detectedEngine() + " typically requires " + toolName + ". " +
                    "Use read-only listing first. Do not extract or rebuild archives without backups.",
                    75,
                    "Set up " + toolName + " in a separate sandbox before touching real game data.",
                    RiskLevel.MEDIUM,
                    List.of()
                ));
            }
        }

        if (savesExist) {
            out.add(new Recommendation(
                RecommendationType.BUILD_SAVE_BACKUP,
                "Save backup tooling candidate",
                "Save data was detected. A simple save-backup workflow is a realistic first quality-of-life feature.",
                70,
                "Copy the save folder to a versioned backup location before any other modding work.",
                RiskLevel.LOW,
                List.of()
            ));
        }

        if (triage.scanConfidence() < 60) {
            out.add(0, new Recommendation(
                RecommendationType.RUN_DEEP_SCAN,
                "Run a deep scan",
                "Quick triage produced low confidence. A deep scan will hash files, scan binary strings, and read text hints across the install folder.",
                80,
                "Pick this game on the dashboard and choose Deep Scan.",
                RiskLevel.LOW,
                List.of()
            ));
        }

        if (!knownEngine && !looseHeavy && triage.packedArchiveSurface() == SurfaceLevel.NONE) {
            out.add(new Recommendation(
                RecommendationType.LOW_VALUE_TARGET,
                "Likely low-value modding target",
                "No known engine signature, no loose content surface, and no recognizable archives were detected. " +
                "There may be little to investigate here.",
                60,
                "Skip this game unless you have a specific reason to look closer.",
                RiskLevel.LOW,
                List.of()
            ));
        }

        if (!knownEngine && (configRich || archiveHeavy)) {
            out.add(new Recommendation(
                RecommendationType.MANUAL_REVIEW,
                "Manual review needed",
                "Content surface looks promising, but the engine could not be identified from the file layout.",
                55,
                "Inspect a few representative files manually and compare with known engine layouts.",
                RiskLevel.LOW,
                List.of()
            ));
        }

        if (triage.badges().contains(Badge.HARD_TARGET)) {
            // No additional rec — the HARD_TARGET badge speaks for itself in the UI.
        }

        out.sort(Comparator.comparingInt(Recommendation::confidence).reversed());
        return List.copyOf(out);
    }

    public static List<Recommendation> evaluate(ScanResult result) {
        // Adapt a full ScanResult into a synthetic triage shape so we don't duplicate rules.
        long total = Math.max(1, result.files().size());
        long configs = result.files().stream()
            .filter(f -> f.category() == FileCategory.CONFIG || f.category() == FileCategory.TEXT)
            .count();
        long archives = result.files().stream()
            .filter(f -> f.category() == FileCategory.ARCHIVE)
            .count();
        long vendor = result.files().stream()
            .filter(f -> f.category() == FileCategory.VENDOR_LIBRARY
                || f.category() == FileCategory.NVIDIA_LIBRARY
                || f.category() == FileCategory.STEAM_LIBRARY
                || f.category() == FileCategory.GRAPHICS_LIBRARY
                || f.category() == FileCategory.STREAMLINE_LIBRARY
                || f.category() == FileCategory.PHYSX_LIBRARY
                || f.category() == FileCategory.DIRECTSTORAGE_LIBRARY
                || f.category() == FileCategory.SYSTEM_COMPAT_LIBRARY
                || f.category() == FileCategory.RUNTIME_LIBRARY)
            .count();
        SurfaceLevel configSurface = scale(configs);
        SurfaceLevel archiveSurface = scale(archives);
        SurfaceLevel looseSurface = scale((total - vendor - archives));
        boolean hasSaves = result.saves().stream().anyMatch(s -> s.exists() && s.sizeBytes() > 0);

        List<Badge> badges = new ArrayList<>();
        if (result.engineDetection().primary() == EngineFamily.UNKNOWN) badges.add(Badge.UNKNOWN_ENGINE);

        GameTriageResult adapted = new GameTriageResult(
            result.install().map(i -> i.profile().id()).orElse("unknown"),
            result.install().map(i -> i.profile().displayName()).orElse("Unknown"),
            result.install().map(i -> i.installPath()).orElse(null),
            com.acltabontabon.modscope.library.GameSource.UNKNOWN,
            result.engineDetection().primary(),
            result.engineDetection().confidence(),
            List.of(),
            configSurface,
            hasSaves ? SurfaceLevel.MEDIUM : SurfaceLevel.NONE,
            looseSurface,
            archiveSurface,
            archiveSurface == SurfaceLevel.HIGH ? ExternalToolDependency.LIKELY : ExternalToolDependency.NONE,
            RecommendationType.MANUAL_REVIEW,
            85,
            java.time.Instant.now(),
            badges,
            result.files().size(),
            0L,
            ""
        );

        List<Recommendation> recs = new ArrayList<>(evaluate(adapted));
        if (vendor > 0 && (double) vendor / total > 0.25) {
            recs.add(new Recommendation(
                RecommendationType.IGNORE_VENDOR_DLL_NOISE,
                "Ignore vendor/runtime DLL noise",
                "A significant share of files are vendor or runtime libraries (NVIDIA, DirectX, Steam, etc.). " +
                "These look noisy in binary scans but rarely contain modding surface.",
                70,
                "Filter vendor categories out of file inventory views when triaging.",
                RiskLevel.LOW,
                List.of()
            ));
        }
        recs.sort(Comparator.comparingInt(Recommendation::confidence).reversed());
        return List.copyOf(recs);
    }

    private static SurfaceLevel scale(long count) {
        if (count <= 0) return SurfaceLevel.NONE;
        if (count < 10) return SurfaceLevel.LOW;
        if (count < 50) return SurfaceLevel.MEDIUM;
        return SurfaceLevel.HIGH;
    }

    private static String externalToolName(EngineFamily family) {
        return switch (family) {
            case GLACIER -> "Glacier/RPKG tooling";
            case UNREAL -> "an Unreal pak/IoStore extractor";
            case CREATION -> "a Bethesda Archive (BA2/BSA) tool";
            case REDENGINE -> "a REDengine/.archive tool (WolvenKit etc.)";
            case ANVIL -> "an AnvilNext .forge tool";
            case UNITY -> "a Unity asset bundle tool";
            default -> null;
        };
    }
}
