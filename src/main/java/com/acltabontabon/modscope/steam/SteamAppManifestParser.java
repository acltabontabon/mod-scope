package com.acltabontabon.modscope.steam;

import com.acltabontabon.modscope.util.SafeIo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SteamAppManifestParser {

    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*\"([^\"]+)\"\\s+\"([^\"]*)\"\\s*$");

    private SteamAppManifestParser() {}

    public static Optional<SteamAppManifest> parse(Path acfFile) {
        List<String> lines = SafeIo.readLines(acfFile);
        if (lines.isEmpty()) return Optional.empty();

        Map<String, String> fields = new HashMap<>();
        for (String line : lines) {
            Matcher m = KV_PATTERN.matcher(line);
            if (m.matches()) {
                fields.put(m.group(1).toLowerCase(), m.group(2));
            }
        }

        String appId = fields.get("appid");
        String name = fields.get("name");
        String installDir = fields.get("installdir");
        if (appId == null || installDir == null) return Optional.empty();

        return Optional.of(new SteamAppManifest(
            appId,
            name != null ? name : "",
            installDir,
            fields.getOrDefault("stateflags", ""),
            acfFile.getParent()
        ));
    }
}
