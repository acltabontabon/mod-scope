package com.acltabontabon.modscope.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class SafeIo {

    private SafeIo() {}

    public static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static Optional<String> readString(Path path) {
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Stream<Path> walk(Path dir) {
        try {
            return Files.walk(dir);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    public static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    public static String lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant().toString();
        } catch (IOException e) {
            return "";
        }
    }
}
