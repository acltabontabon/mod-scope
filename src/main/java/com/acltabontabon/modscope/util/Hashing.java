package com.acltabontabon.modscope.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

public final class Hashing {

    public static final long DEFAULT_HASH_LIMIT_BYTES = 100L * 1024 * 1024; // 100 MB

    private Hashing() {}

    public static Optional<String> sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    digest.update(buf, 0, read);
                }
            }
            return Optional.of(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException | IOException e) {
            return Optional.empty();
        }
    }
}
