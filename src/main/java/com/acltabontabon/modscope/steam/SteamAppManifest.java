package com.acltabontabon.modscope.steam;

import java.nio.file.Path;

public record SteamAppManifest(
    String appId,
    String name,
    String installDir,
    String stateFlags,
    Path steamappsDir
) {
    public Path resolvedInstallPath() {
        return steamappsDir.resolve("common").resolve(installDir);
    }
}
