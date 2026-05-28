package com.acltabontabon.modscope.report;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.scan.FileEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FileInventoryJsonWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private FileInventoryJsonWriter() {}

    public static void write(Path outputFile, ScanResult result) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scannedAt", result.scannedAt());
        report.put("gameProfile", result.options().profileId() != null
            ? result.options().profileId()
            : result.install().map(i -> i.profile().id()).orElse("unknown"));
        report.put("installPath", result.install()
            .map(i -> i.installPath().toString())
            .orElse(null));
        report.put("scanMode", result.options().mode().name());
        report.put("totalFiles", result.files().size());

        List<Map<String, Object>> files = new ArrayList<>();
        for (FileEntry entry : result.files()) {
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("relativePath", entry.relativePath());
            file.put("category", entry.category().name());
            file.put("extension", entry.extension());
            file.put("sizeBytes", entry.sizeBytes());
            file.put("lastModified", entry.lastModified());
            if (entry.sha256() != null) file.put("sha256", entry.sha256());
            if (entry.hashSkippedReason() != null) file.put("hashSkippedReason", entry.hashSkippedReason());
            files.add(file);
        }
        report.put("files", files);

        Files.createDirectories(outputFile.getParent());
        MAPPER.writeValue(outputFile.toFile(), report);
    }
}
