package com.acltabontabon.modscope.game;

import com.acltabontabon.modscope.core.ScanOptions;
import com.acltabontabon.modscope.steam.SteamLibraryScanner;
import com.acltabontabon.modscope.steam.SteamLocator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class GameInstallDetector {

    private GameInstallDetector() {}

    public static Optional<GameInstall> detect(GameProfile profile, ScanOptions options) {
        Path installPath;

        if (options.gameDir() != null) {
            installPath = options.gameDir();
        } else {
            Optional<Path> fromSteam = detectViaSteam(profile);
            if (fromSteam.isEmpty()) return Optional.empty();
            installPath = fromSteam.get();
        }

        if (!Files.isDirectory(installPath)) return Optional.empty();

        GamePathValidator.ValidationResult validation = GamePathValidator.validate(installPath, profile);
        if (!validation.valid()) {
            // Still return the install if the directory exists, just with empty detections
            return Optional.of(new GameInstall(profile, installPath, validation.subfolders(), validation.executables()));
        }

        return Optional.of(new GameInstall(
            profile,
            installPath,
            validation.subfolders(),
            validation.executables()
        ));
    }

    private static Optional<Path> detectViaSteam(GameProfile profile) {
        return SteamLocator.findSteamRoot()
            .map(root -> SteamLibraryScanner.findLibraryFolders(root))
            .flatMap(libs -> SteamLibraryScanner.findAppInstallDir(libs, profile.steamAppId()));
    }
}
