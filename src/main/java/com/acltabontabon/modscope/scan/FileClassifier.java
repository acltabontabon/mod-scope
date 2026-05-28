package com.acltabontabon.modscope.scan;

import java.util.Set;

public final class FileClassifier {

    private static final Set<String> GAME_EXECUTABLE_EXTS = Set.of("exe", "bin", "elf");
    private static final Set<String> LIBRARY_EXTS = Set.of("dll", "so", "dylib");

    private static final Set<String> STEAM_LIBRARY_NAMES = Set.of(
        "steam_api.dll", "steam_api64.dll", "steamclient.dll", "steamclient64.dll",
        "tier0_s.dll", "vstdlib_s.dll", "steam_emu.dll"
    );

    private static final Set<String> ARCHIVE_EXTS = Set.of(
        "pak", "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
        "arc", "bundle", "ba2", "bsa", "upk", "uasset",
        "tiger", "rpkg", "forge", "big", "cab",
        "ucas", "utoc"
    );
    private static final Set<String> CONFIG_EXTS = Set.of(
        "cfg", "config", "ini", "toml", "properties", "conf",
        "settings", "options", "pref", "prefs", "reg"
    );
    private static final Set<String> TEXT_EXTS = Set.of(
        "txt", "json", "xml", "yaml", "yml", "csv", "log",
        "md", "lua", "py", "js", "ts", "html", "htm",
        "sql", "manifest", "info", "nfo"
    );
    private static final Set<String> VIDEO_EXTS = Set.of(
        "mp4", "mov", "avi", "mkv", "wmv", "bik", "bik2",
        "webm", "ogv", "m4v", "usm", "vp8", "vp9", "flv",
        "mpeg", "mpg", "sfd", "ivf"
    );
    private static final Set<String> LOCALIZATION_EXTS = Set.of(
        "loc", "l10n", "po", "pot", "xliff", "strings",
        "resx", "lang", "language", "translation", "locale"
    );
    private static final Set<String> SHADER_EXTS = Set.of(
        "cache", "shaderdb", "hlsl", "glsl", "spv", "fxc",
        "sb", "pso", "cso", "dxbc", "dxil", "metallib"
    );

    private static final long LARGE_FILE_THRESHOLD = 500L * 1024 * 1024;

    private FileClassifier() {}

    public static FileCategory classify(String filename, String extension, long sizeBytes) {
        String ext = extension.toLowerCase();
        String name = filename.toLowerCase();

        // packagedefinition.txt is a Glacier engine chunk manifest
        if (name.equals("packagedefinition.txt")) return FileCategory.PACKAGE_DEFINITION;

        if (GAME_EXECUTABLE_EXTS.contains(ext)) return FileCategory.GAME_EXECUTABLE;

        if (LIBRARY_EXTS.contains(ext)) {
            if (STEAM_LIBRARY_NAMES.contains(name)) return FileCategory.STEAM_LIBRARY;
            if (name.startsWith("nv") || name.startsWith("cuda") || name.startsWith("nvapi")) {
                return FileCategory.NVIDIA_LIBRARY;
            }
            return FileCategory.RUNTIME_LIBRARY;
        }

        if (ARCHIVE_EXTS.contains(ext)) return FileCategory.ARCHIVE;
        if (CONFIG_EXTS.contains(ext)) return FileCategory.CONFIG;
        if (VIDEO_EXTS.contains(ext)) return FileCategory.VIDEO;
        if (LOCALIZATION_EXTS.contains(ext)) return FileCategory.LOCALIZATION;
        if (SHADER_EXTS.contains(ext)) return FileCategory.SHADER_CACHE;
        if (TEXT_EXTS.contains(ext)) return FileCategory.TEXT;
        if (sizeBytes > LARGE_FILE_THRESHOLD) return FileCategory.UNKNOWN_LARGE;
        return FileCategory.OTHER;
    }

    public static boolean isTextReadable(String extension) {
        String ext = extension.toLowerCase();
        return CONFIG_EXTS.contains(ext) || TEXT_EXTS.contains(ext);
    }

    public static boolean isBinaryScannable(FileCategory category) {
        return category == FileCategory.ARCHIVE
            || category == FileCategory.GAME_EXECUTABLE
            || category == FileCategory.RUNTIME_LIBRARY
            || category == FileCategory.UNKNOWN_LARGE;
    }
}
