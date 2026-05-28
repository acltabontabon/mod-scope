package com.acltabontabon.modscope.library;

import com.acltabontabon.modscope.steam.SteamAppManifest;
import com.acltabontabon.modscope.steam.SteamLibraryScanner;
import com.acltabontabon.modscope.steam.SteamLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Enumerates installed games across all supported stores.
 * Steam is the only implemented backend today; EPIC/GOG return empty lists.
 */
public final class DetectedGameRegistry {

    private static final int MAX_EXE_CANDIDATES = 8;
    private static final int MAX_EXE_SCAN_DEPTH = 2;

    private DetectedGameRegistry() {}

    public static List<DetectedGame> detectAll() {
        List<DetectedGame> all = new ArrayList<>();
        all.addAll(detectSteam());
        all.addAll(detectEpic());
        all.addAll(detectGog());
        all.sort(Comparator.comparing(DetectedGame::displayName, String.CASE_INSENSITIVE_ORDER));
        return all;
    }

    public static List<DetectedGame> detectSteam() {
        Optional<Path> steamRoot = SteamLocator.findSteamRoot();
        if (steamRoot.isEmpty()) return List.of();
        List<Path> libraries = SteamLibraryScanner.findLibraryFolders(steamRoot.get());
        List<SteamAppManifest> manifests = SteamLibraryScanner.allManifests(libraries);
        List<DetectedGame> games = new ArrayList<>(manifests.size());
        for (SteamAppManifest m : manifests) {
            games.add(fromSteam(m));
        }
        return games;
    }

    public static List<DetectedGame> detectEpic() {
        // Placeholder: future Epic Games Launcher manifest parsing goes here.
        return List.of();
    }

    public static List<DetectedGame> detectGog() {
        // Placeholder: future GOG Galaxy database parsing goes here.
        return List.of();
    }

    private static DetectedGame fromSteam(SteamAppManifest manifest) {
        Path installPath = manifest.resolvedInstallPath();
        List<Path> exes = findExecutables(installPath);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("steam.installDir", manifest.installDir());
        metadata.put("steam.stateFlags", manifest.stateFlags());
        Path manifestPath = manifest.steamappsDir().resolve("appmanifest_" + manifest.appId() + ".acf");
        return new DetectedGame(
            "steam-" + manifest.appId(),
            manifest.name(),
            GameSource.STEAM,
            manifest.appId(),
            installPath,
            exes,
            manifestPath,
            Instant.now(),
            Files.isDirectory(installPath) ? 95 : 40,
            Map.copyOf(metadata)
        );
    }

    private static List<Path> findExecutables(Path installPath) {
        if (!Files.isDirectory(installPath)) return List.of();
        List<Path> exes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(installPath, MAX_EXE_SCAN_DEPTH)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".exe");
                })
                .limit(MAX_EXE_CANDIDATES * 4L)
                .forEach(exes::add);
        } catch (IOException ignored) {
            return List.of();
        }
        // Prefer top-level exes that look like a game launcher (no "redist"/"setup" hints).
        exes.sort(Comparator.<Path>comparingInt(p -> p.getNameCount()).thenComparing(Path::toString));
        List<Path> filtered = new ArrayList<>();
        for (Path p : exes) {
            String n = p.getFileName().toString().toLowerCase();
            if (n.contains("setup") || n.contains("redist") || n.contains("vc_") || n.contains("unins")) continue;
            filtered.add(p);
            if (filtered.size() >= MAX_EXE_CANDIDATES) break;
        }
        return List.copyOf(filtered);
    }
}
