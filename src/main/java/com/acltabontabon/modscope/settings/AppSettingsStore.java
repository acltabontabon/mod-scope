package com.acltabontabon.modscope.settings;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.storage.AppPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AppSettingsStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path file;

    public AppSettingsStore() {
        this(AppPaths.settingsPath());
    }

    public AppSettingsStore(Path file) {
        this.file = file;
    }

    public Path file() { return file; }

    @SuppressWarnings("unchecked")
    public AppSettings load() {
        if (!Files.isRegularFile(file)) return AppSettings.defaults();
        try {
            Map<String, Object> m = MAPPER.readValue(file.toFile(), Map.class);
            AppSettings d = AppSettings.defaults();
            return new AppSettings(
                m.containsKey("reportsDir") ? Path.of(str(m.get("reportsDir"))) : d.reportsDir(),
                m.containsKey("defaultScanMode") ? parseMode(str(m.get("defaultScanMode")), d.defaultScanMode()) : d.defaultScanMode(),
                bool(m, "includeBinaryStringScanByDefault", d.includeBinaryStringScanByDefault()),
                bool(m, "includeVendorRuntimeLibs", d.includeVendorRuntimeLibs()),
                bool(m, "includeGameExecutableStrings", d.includeGameExecutableStrings()),
                bool(m, "includeLargeArchiveSampling", d.includeLargeArchiveSampling()),
                intVal(m, "maxStringSampleMb", d.maxStringSampleMb()),
                intVal(m, "maxHashSizeMb", d.maxHashSizeMb()),
                bool(m, "showAdvancedWarnings", d.showAdvancedWarnings())
            );
        } catch (IOException | ClassCastException e) {
            return AppSettings.defaults();
        }
    }

    public void save(AppSettings settings) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reportsDir", settings.reportsDir().toString());
        m.put("defaultScanMode", settings.defaultScanMode().name());
        m.put("includeBinaryStringScanByDefault", settings.includeBinaryStringScanByDefault());
        m.put("includeVendorRuntimeLibs", settings.includeVendorRuntimeLibs());
        m.put("includeGameExecutableStrings", settings.includeGameExecutableStrings());
        m.put("includeLargeArchiveSampling", settings.includeLargeArchiveSampling());
        m.put("maxStringSampleMb", settings.maxStringSampleMb());
        m.put("maxHashSizeMb", settings.maxHashSizeMb());
        m.put("showAdvancedWarnings", settings.showAdvancedWarnings());
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), m);
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static boolean bool(Map<String, Object> m, String key, boolean fallback) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        if (v != null) return Boolean.parseBoolean(v.toString());
        return fallback;
    }

    private static int intVal(Map<String, Object> m, String key, int fallback) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static ScanMode parseMode(String s, ScanMode fallback) {
        if (s == null) return fallback;
        try { return ScanMode.valueOf(s); } catch (IllegalArgumentException e) { return fallback; }
    }
}
