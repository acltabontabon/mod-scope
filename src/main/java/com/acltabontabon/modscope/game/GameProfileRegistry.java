package com.acltabontabon.modscope.game;

import com.acltabontabon.modscope.game.profiles.FirstLightProfile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GameProfileRegistry {

    private static final Map<String, GameProfile> PROFILES = new LinkedHashMap<>();

    static {
        register(FirstLightProfile.INSTANCE);
    }

    private GameProfileRegistry() {}

    public static void register(GameProfile profile) {
        PROFILES.put(profile.id(), profile);
    }

    public static Optional<GameProfile> findById(String id) {
        return Optional.ofNullable(PROFILES.get(id));
    }

    public static Collection<GameProfile> all() {
        return PROFILES.values();
    }
}
