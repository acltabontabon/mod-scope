package com.acltabontabon.modscope.scan;

import com.acltabontabon.modscope.util.SafeIo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PackageDefinitionAnalyzer {

    private PackageDefinitionAnalyzer() {}

    public static PackageDefinitionAnalysis analyze(Path file) {
        List<String> lines = SafeIo.readLines(file);
        if (lines.isEmpty()) return PackageDefinitionAnalysis.notFound();

        String assemblyPath = null;
        String currentSection = "DEFAULT";
        List<PackageDefinitionEntry> entries = new ArrayList<>();

        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;

            // Assembly header: [assembly:/...]
            if (line.startsWith("[") && line.endsWith("]")) {
                String inner = line.substring(1, line.length() - 1).strip();
                if (inner.startsWith("assembly:") && assemblyPath == null) {
                    assemblyPath = inner;
                }
                continue;
            }

            // Section marker: @SECTIONNAME
            if (line.startsWith("@")) {
                currentSection = line.substring(1).strip();
                continue;
            }

            // Key-value entry: chunkName = resourcePath
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (!key.isEmpty() && !value.isEmpty()) {
                    entries.add(new PackageDefinitionEntry(key, value, currentSection));
                }
            }
        }

        return new PackageDefinitionAnalysis(true, assemblyPath, List.copyOf(entries));
    }
}
