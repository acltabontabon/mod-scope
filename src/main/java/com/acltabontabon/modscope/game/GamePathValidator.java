package com.acltabontabon.modscope.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GamePathValidator {

    private GamePathValidator() {}

    public static ValidationResult validate(Path dir, GameProfile profile) {
        if (!Files.isDirectory(dir)) {
            return ValidationResult.invalid("Path does not exist or is not a directory: " + dir);
        }

        List<Path> foundExecutables = new ArrayList<>();
        for (String exeCandidate : profile.executableCandidates()) {
            Path exe = dir.resolve(exeCandidate.replace("/", dir.getFileSystem().getSeparator()));
            if (Files.isRegularFile(exe)) {
                foundExecutables.add(exe);
            }
        }

        List<Path> foundSubfolders = new ArrayList<>();
        for (String subfolder : profile.inspectSubfolders()) {
            Path sub = dir.resolve(subfolder);
            if (Files.isDirectory(sub)) {
                foundSubfolders.add(sub);
            }
        }

        if (foundExecutables.isEmpty() && foundSubfolders.isEmpty()) {
            return ValidationResult.invalid(
                "Directory exists but no expected executables or subfolders were found. " +
                "This may not be the correct game folder."
            );
        }

        return ValidationResult.valid(foundExecutables, foundSubfolders);
    }

    public record ValidationResult(
        boolean valid,
        String reason,
        List<Path> executables,
        List<Path> subfolders
    ) {
        static ValidationResult valid(List<Path> exes, List<Path> subs) {
            return new ValidationResult(true, null, exes, subs);
        }

        static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, List.of(), List.of());
        }
    }
}
