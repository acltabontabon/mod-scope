package com.acltabontabon.modscope.scan;

import java.util.Set;

public final class FileClassifier {

    private static final Set<String> GAME_EXECUTABLE_EXTS = Set.of("exe", "bin", "elf");
    private static final Set<String> LIBRARY_EXTS = Set.of("dll", "so", "dylib");

    private static final Set<String> STEAM_LIBRARY_NAMES = Set.of(
        "steam_api.dll", "steam_api64.dll", "steamclient.dll", "steamclient64.dll",
        "tier0_s.dll", "vstdlib_s.dll", "steam_emu.dll"
    );

    // Direct3D, DXGI, Vulkan, and generic graphics runtimes
    private static final Set<String> GRAPHICS_LIBRARY_NAMES = Set.of(
        "d3d12core.dll", "d3d12.dll", "d3d11.dll", "d3d10.dll", "d3d9.dll",
        "dxgi.dll", "dxcore.dll", "dxcompiler.dll", "vulkan-1.dll",
        "opengl32.dll", "libgl.so", "libvulkan.so"
    );
    private static final Set<String> GRAPHICS_LIBRARY_PREFIXES = Set.of(
        "d3dcompiler_", "d3dx12", "d3dx11", "d3dx9"
    );

    // NVIDIA Streamline overlay DLLs (sl.<feature>.dll)
    private static final String STREAMLINE_PREFIX = "sl.";

    // PhysX SDK DLLs
    private static final Set<String> PHYSX_PREFIXES = Set.of(
        "physx", "nvphysx", "pxfoundation", "pxpvdsdk", "pxcuda"
    );

    // DirectStorage runtime
    private static final Set<String> DIRECTSTORAGE_NAMES = Set.of(
        "dstorage.dll", "dstoragecore.dll", "directstorage.dll"
    );

    // MSVC/CRT system compat DLLs
    private static final Set<String> SYSTEM_COMPAT_PREFIXES = Set.of(
        "vcruntime", "msvcp", "msvcr", "ucrtbase", "concrt"
    );
    private static final Set<String> SYSTEM_COMPAT_NAMES = Set.of(
        "msvcrt.dll", "ucrtbase.dll"
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

        if (name.equals("packagedefinition.txt")) return FileCategory.PACKAGE_DEFINITION;

        if (GAME_EXECUTABLE_EXTS.contains(ext)) return FileCategory.GAME_EXECUTABLE;

        if (LIBRARY_EXTS.contains(ext)) {
            return classifyLibrary(name);
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

    private static FileCategory classifyLibrary(String name) {
        // Check most specific first
        if (STEAM_LIBRARY_NAMES.contains(name)) return FileCategory.STEAM_LIBRARY;
        if (GRAPHICS_LIBRARY_NAMES.contains(name)) return FileCategory.GRAPHICS_LIBRARY;
        for (String prefix : GRAPHICS_LIBRARY_PREFIXES) {
            if (name.startsWith(prefix)) return FileCategory.GRAPHICS_LIBRARY;
        }
        if (DIRECTSTORAGE_NAMES.contains(name)) return FileCategory.DIRECTSTORAGE_LIBRARY;
        if (name.startsWith(STREAMLINE_PREFIX)) return FileCategory.STREAMLINE_LIBRARY;
        for (String prefix : PHYSX_PREFIXES) {
            if (name.startsWith(prefix)) return FileCategory.PHYSX_LIBRARY;
        }
        // NVIDIA: nv* and cuda* (excluding nvphysx which is matched above)
        if (name.startsWith("nv") || name.startsWith("cuda") || name.startsWith("nvapi")) {
            return FileCategory.NVIDIA_LIBRARY;
        }
        if (SYSTEM_COMPAT_NAMES.contains(name)) return FileCategory.SYSTEM_COMPAT_LIBRARY;
        for (String prefix : SYSTEM_COMPAT_PREFIXES) {
            if (name.startsWith(prefix)) return FileCategory.SYSTEM_COMPAT_LIBRARY;
        }
        return FileCategory.RUNTIME_LIBRARY;
    }

    public static boolean isTextReadable(String extension) {
        String ext = extension.toLowerCase();
        return CONFIG_EXTS.contains(ext) || TEXT_EXTS.contains(ext);
    }

    public static boolean isVendorLibrary(FileCategory category) {
        return switch (category) {
            case RUNTIME_LIBRARY, NVIDIA_LIBRARY, STEAM_LIBRARY,
                 GRAPHICS_LIBRARY, STREAMLINE_LIBRARY, PHYSX_LIBRARY,
                 DIRECTSTORAGE_LIBRARY, SYSTEM_COMPAT_LIBRARY, VENDOR_LIBRARY -> true;
            default -> false;
        };
    }
}
