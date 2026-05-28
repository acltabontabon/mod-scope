package com.acltabontabon.modscope.scan;

import java.util.List;

public record PackageDefinitionAnalysis(
    boolean found,
    String assemblyPath,
    List<PackageDefinitionEntry> entries
) {
    public static PackageDefinitionAnalysis notFound() {
        return new PackageDefinitionAnalysis(false, null, List.of());
    }

    public int chunkCount() {
        return entries.size();
    }
}
