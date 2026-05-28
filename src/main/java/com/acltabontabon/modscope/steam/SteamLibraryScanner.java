package com.acltabontabon.modscope.steam;

import com.acltabontabon.modscope.util.SafeIo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SteamLibraryScanner {

    private static final Pattern PATH_PATTERN = Pattern.compile("^\\s*\"path\"\\s+\"([^\"]*)\"\\s*$");

    private SteamLibraryScanner() {}

    public static List<Path> findLibraryFolders(Path steamRoot) {
        List<Path> libraries = new ArrayList<>();
        libraries.add(steamRoot);

        Path vdfFile = steamRoot.resolve("config").resolve("libraryfolders.vdf");
        if (!Files.isRegularFile(vdfFile)) {
            vdfFile = steamRoot.resolve("steamapps").resolve("libraryfolders.vdf");
        }

        List<String> lines = SafeIo.readLines(vdfFile);
        for (String line : lines) {
            Matcher m = PATH_PATTERN.matcher(line);
            if (m.matches()) {
                String rawPath = m.group(1).replace("\\\\", "\\");
                Path lib = Path.of(rawPath);
                if (Files.isDirectory(lib) && !libraries.contains(lib)) {
                    libraries.add(lib);
                }
            }
        }
        return libraries;
    }

    public static Optional<Path> findAppInstallDir(List<Path> libraries, String appId) {
        String manifestName = "appmanifest_" + appId + ".acf";
        for (Path lib : libraries) {
            Path steamapps = lib.resolve("steamapps");
            Path manifest = steamapps.resolve(manifestName);
            if (Files.isRegularFile(manifest)) {
                return SteamAppManifestParser.parse(manifest)
                    .map(m -> steamapps.resolve("common").resolve(m.installDir()));
            }
        }
        return Optional.empty();
    }
}
