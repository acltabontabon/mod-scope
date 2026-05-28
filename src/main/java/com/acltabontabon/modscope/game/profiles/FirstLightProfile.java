package com.acltabontabon.modscope.game.profiles;

import com.acltabontabon.modscope.game.GameProfile;

import java.util.List;

public final class FirstLightProfile {

    public static final GameProfile INSTANCE = new GameProfile(
        "007-first-light",
        "007 First Light",
        "3768760",
        List.of("007 First Light"),
        List.of(
            "Retail/Game.exe",
            "Retail/007FirstLight.exe",
            "Game.exe",
            "007FirstLight.exe"
        ),
        List.of("Retail", "Runtime", "Content", "Movies", "Videos", "Config", "Saved"),
        List.of(
            "{steamRoot}/userdata/{steamUserId}/3768760/",
            "{steamRoot}/userdata/{steamUserId}/3768760/remote/"
        )
    );

    private FirstLightProfile() {}
}
