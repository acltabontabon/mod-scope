package com.acltabontabon.modscope.util;

import java.nio.file.Path;

public final class PathUtils {

    private PathUtils() {}

    public static Path expandHome(String path) {
        if (path.equals("~") || path.startsWith("~/") || path.startsWith("~\\")) {
            return Path.of(System.getProperty("user.home"), path.substring(1));
        }
        return Path.of(path);
    }

    public static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase();
    }

    public static String safeRelativize(Path base, Path file) {
        try {
            return base.relativize(file).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return file.toString().replace('\\', '/');
        }
    }
}
