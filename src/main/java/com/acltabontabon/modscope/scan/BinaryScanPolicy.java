package com.acltabontabon.modscope.scan;

public record BinaryScanPolicy(
    boolean includeGameExecutable,
    boolean includeVendorLibraries,
    boolean includeLargeArchives
) {
    // Archives beyond this size require includeLargeArchives=true
    public static final long LARGE_ARCHIVE_THRESHOLD = 128L * 1024 * 1024;

    public static BinaryScanPolicy conservative() {
        return new BinaryScanPolicy(false, false, false);
    }

    public boolean shouldScan(FileCategory category, long sizeBytes) {
        return switch (category) {
            case ARCHIVE, PACKAGE_DEFINITION -> {
                if (sizeBytes > LARGE_ARCHIVE_THRESHOLD && !includeLargeArchives) yield false;
                yield true;
            }
            case GAME_EXECUTABLE -> includeGameExecutable;
            case RUNTIME_LIBRARY, NVIDIA_LIBRARY, STEAM_LIBRARY,
                 GRAPHICS_LIBRARY, STREAMLINE_LIBRARY, PHYSX_LIBRARY,
                 DIRECTSTORAGE_LIBRARY, SYSTEM_COMPAT_LIBRARY, VENDOR_LIBRARY -> includeVendorLibraries;
            case UNKNOWN_LARGE -> includeLargeArchives;
            case OTHER -> true;
            default -> false;
        };
    }

    public String description() {
        if (includeVendorLibraries && includeGameExecutable && includeLargeArchives) return "all files";
        var sb = new StringBuilder();
        if (!includeVendorLibraries) sb.append("vendor/runtime excluded");
        if (!includeGameExecutable) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("game exe excluded");
        }
        if (!includeLargeArchives) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("large archives excluded");
        }
        return sb.isEmpty() ? "default" : sb.toString();
    }
}
