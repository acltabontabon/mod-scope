package com.acltabontabon.modscope.steam;

public record SteamAppManifest(
    String appId,
    String name,
    String installDir,
    String stateFlags
) {}
