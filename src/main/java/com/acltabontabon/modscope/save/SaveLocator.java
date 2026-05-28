package com.acltabontabon.modscope.save;

import com.acltabontabon.modscope.game.GameProfile;
import com.acltabontabon.modscope.steam.SteamLocator;
import com.acltabontabon.modscope.util.SafeIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SaveLocator {

    private SaveLocator() {}

    public static List<SaveCandidate> locate(GameProfile profile) {
        Optional<Path> steamRoot = SteamLocator.findSteamRoot();
        if (steamRoot.isEmpty()) return List.of();

        Path userdataDir = steamRoot.get().resolve("userdata");
        if (!Files.isDirectory(userdataDir)) return List.of();

        List<String> userIds = listUserIds(userdataDir);
        List<SaveCandidate> candidates = new ArrayList<>();

        for (String userId : userIds) {
            for (String template : profile.savePathTemplates()) {
                String resolved = template
                    .replace("{steamRoot}", steamRoot.get().toString())
                    .replace("{steamUserId}", userId)
                    .replace("/", steamRoot.get().getFileSystem().getSeparator());
                Path path = Path.of(resolved);
                candidates.add(buildCandidate(path));
            }
        }

        return candidates;
    }

    private static SaveCandidate buildCandidate(Path path) {
        boolean exists = Files.exists(path);
        long size = 0;
        String lastModified = "";
        if (exists) {
            size = directorySize(path);
            lastModified = SafeIo.lastModified(path);
        }
        return new SaveCandidate(path, exists, size, lastModified);
    }

    private static List<String> listUserIds(Path userdataDir) {
        List<String> ids = new ArrayList<>();
        try (var stream = Files.list(userdataDir)) {
            stream.filter(Files::isDirectory)
                  .map(p -> p.getFileName().toString())
                  .filter(name -> name.matches("\\d+"))
                  .forEach(ids::add);
        } catch (IOException ignored) {}
        return ids;
    }

    private static long directorySize(Path path) {
        try (var stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile)
                         .mapToLong(f -> {
                             try { return Files.size(f); } catch (IOException e) { return 0; }
                         })
                         .sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
