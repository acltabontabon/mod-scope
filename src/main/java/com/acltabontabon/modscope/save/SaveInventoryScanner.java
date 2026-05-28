package com.acltabontabon.modscope.save;

import com.acltabontabon.modscope.util.SafeIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SaveInventoryScanner {

    private SaveInventoryScanner() {}

    public static List<SaveFileEntry> inventory(List<SaveCandidate> candidates) {
        List<SaveFileEntry> entries = new ArrayList<>();
        for (SaveCandidate candidate : candidates) {
            if (!candidate.exists()) continue;
            Path dir = candidate.path();
            if (Files.isDirectory(dir)) {
                entries.addAll(walkDirectory(dir));
            } else if (Files.isRegularFile(dir)) {
                entries.add(toEntry(dir));
            }
        }
        return entries;
    }

    private static List<SaveFileEntry> walkDirectory(Path dir) {
        List<SaveFileEntry> entries = new ArrayList<>();
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(f -> entries.add(toEntry(f)));
        } catch (IOException ignored) {}
        return entries;
    }

    private static SaveFileEntry toEntry(Path file) {
        long size = SafeIo.fileSize(file);
        String modified = SafeIo.lastModified(file);
        return new SaveFileEntry(file, file.getFileName().toString(), size, modified);
    }
}
