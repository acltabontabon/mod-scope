package com.acltabontabon.modscope.steam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SteamLocator {

    private SteamLocator() {}

    public static Optional<Path> findSteamRoot() {
        for (Path candidate : getCandidates()) {
            if (Files.isDirectory(candidate) && Files.isDirectory(candidate.resolve("steamapps"))) {
                return Optional.of(candidate);
            }
        }
        return tryRegistryOnWindows();
    }

    public static List<Path> getCandidates() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", "");
        List<Path> candidates = new ArrayList<>();

        if (os.contains("win")) {
            candidates.add(Path.of("C:\\Program Files (x86)\\Steam"));
            candidates.add(Path.of("C:\\Program Files\\Steam"));
            candidates.add(Path.of("D:\\Steam"));
            String steamDir = System.getenv("STEAM_DIR");
            if (steamDir != null) candidates.add(Path.of(steamDir));
        } else if (os.contains("mac")) {
            candidates.add(Path.of(home, "Library", "Application Support", "Steam"));
        } else {
            candidates.add(Path.of(home, ".steam", "steam"));
            candidates.add(Path.of(home, ".local", "share", "Steam"));
            candidates.add(Path.of(home, ".steam", "root"));
            String steamDir = System.getenv("STEAM_DIR");
            if (steamDir != null) candidates.add(Path.of(steamDir));
        }
        return candidates;
    }

    private static Optional<Path> tryRegistryOnWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) return Optional.empty();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", "HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam", "/v", "SteamPath"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            proc.waitFor();
            for (String line : output.split("\\r?\\n")) {
                if (line.contains("SteamPath")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        Path p = Path.of(parts[parts.length - 1]);
                        if (Files.isDirectory(p)) return Optional.of(p);
                    }
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }
}
