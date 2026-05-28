package com.acltabontabon.modscope.scan;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.util.Hashing;
import com.acltabontabon.modscope.util.PathUtils;
import com.acltabontabon.modscope.util.SafeIo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FileInventoryScanner {

    private FileInventoryScanner() {}

    public static List<FileEntry> scan(Path gameDir, ScanMode mode, Consumer<FileEntry> onEach) {
        List<FileEntry> entries = new ArrayList<>();
        try (var stream = SafeIo.walk(gameDir)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                FileEntry entry = classify(gameDir, file, mode);
                entries.add(entry);
                if (onEach != null) onEach.accept(entry);
            });
        }
        return entries;
    }

    private static FileEntry classify(Path base, Path file, ScanMode mode) {
        String relativePath = PathUtils.safeRelativize(base, file);
        String extension = PathUtils.extensionOf(file);
        long size = SafeIo.fileSize(file);
        String lastModified = SafeIo.lastModified(file);
        String filename = file.getFileName().toString();
        FileCategory category = FileClassifier.classify(filename, extension, size);

        long hashLimit = mode == ScanMode.DEEP ? Long.MAX_VALUE : Hashing.DEFAULT_HASH_LIMIT_BYTES;
        String sha256 = null;
        String skipReason = null;

        if (size > hashLimit) {
            skipReason = "file exceeds " + (hashLimit / (1024 * 1024)) + " MB hash limit";
        } else if (size <= 0) {
            skipReason = "empty file";
        } else {
            sha256 = Hashing.sha256(file).orElse(null);
            if (sha256 == null) skipReason = "read error";
        }

        return new FileEntry(relativePath, category, extension, size, lastModified, sha256, skipReason);
    }
}
