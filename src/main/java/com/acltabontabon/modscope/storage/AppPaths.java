package com.acltabontabon.modscope.storage;

import com.acltabontabon.modscope.util.PathUtils;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Resolves user-scoped ModScope storage locations.
 *
 * Layout:
 *   ~/.modscope/
 *     state/        scan-history.json, settings.json
 *     reports/
 *       library-summary.md
 *       games/<safe-id>/...
 *
 * Override the root via the MODSCOPE_HOME environment variable.
 */
public final class AppPaths {

    private static final String ENV_HOME = "MODSCOPE_HOME";
    private static final String DEFAULT_DIR = ".modscope";

    private AppPaths() {}

    public static Path root() {
        String override = System.getenv(ENV_HOME);
        if (override != null && !override.isBlank()) {
            return PathUtils.expandHome(override);
        }
        return Path.of(System.getProperty("user.home"), DEFAULT_DIR);
    }

    public static Path stateDir() {
        return root().resolve("state");
    }

    public static Path reportsRoot() {
        return root().resolve("reports");
    }

    public static Path reportsForGame(String safeGameId) {
        return reportsRoot().resolve("games").resolve(safeGameId);
    }

    public static Path librarySummaryPath() {
        return reportsRoot().resolve("library-summary.md");
    }

    public static Path scanHistoryPath() {
        return stateDir().resolve("scan-history.json");
    }

    public static Path settingsPath() {
        return stateDir().resolve("settings.json");
    }

    /**
     * Build a filesystem-safe identifier from a display name and optional appId.
     * Falls back to steam-&lt;appId&gt; when the name has no usable ASCII characters.
     */
    public static String safeGameId(String displayName, String appId) {
        String slug = slug(displayName);
        if (!slug.isBlank()) return slug;
        if (appId != null && !appId.isBlank()) return "steam-" + appId;
        return "unknown-" + Integer.toHexString(System.identityHashCode(displayName));
    }

    private static String slug(String name) {
        if (name == null) return "";
        StringBuilder out = new StringBuilder(name.length());
        boolean lastDash = true;
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
                lastDash = false;
            } else if (!lastDash) {
                out.append('-');
                lastDash = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.deleteCharAt(out.length() - 1);
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
