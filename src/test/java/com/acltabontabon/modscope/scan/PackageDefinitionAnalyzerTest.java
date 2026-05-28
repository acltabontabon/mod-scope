package com.acltabontabon.modscope.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PackageDefinitionAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesFullGlacierFormat() throws IOException {
        Path file = tempDir.resolve("packagedefinition.txt");
        Files.writeString(file, """
            [assembly:/peacock/globals/peacock.pc_packagedefinition]

            @ALWAYSSEND
            header = header.pc_rp

            @DEFAULT
            chunk0 = chunk0.pc_rp
            chunk1 = chunk1.pc_rp

            @OPTIONAL
            optional0 = optional0.pc_rp
            """, StandardCharsets.UTF_8);

        var analysis = PackageDefinitionAnalyzer.analyze(file);

        assertTrue(analysis.found());
        assertEquals("assembly:/peacock/globals/peacock.pc_packagedefinition", analysis.assemblyPath());
        assertEquals(4, analysis.chunkCount());

        var header = analysis.entries().stream()
            .filter(e -> e.chunkName().equals("header")).findFirst().orElseThrow();
        assertEquals("header.pc_rp", header.resourcePath());
        assertEquals("ALWAYSSEND", header.section());

        var chunk0 = analysis.entries().stream()
            .filter(e -> e.chunkName().equals("chunk0")).findFirst().orElseThrow();
        assertEquals("DEFAULT", chunk0.section());
    }

    @Test
    void skipsCommentLines() throws IOException {
        Path file = tempDir.resolve("packagedefinition.txt");
        Files.writeString(file, """
            # This is a comment
            [assembly:/test.pc_packagedefinition]
            ; another comment
            @DEFAULT
            chunk0 = chunk0.pc_rp
            """, StandardCharsets.UTF_8);

        var analysis = PackageDefinitionAnalyzer.analyze(file);
        assertTrue(analysis.found());
        assertEquals(1, analysis.chunkCount());
    }

    @Test
    void handlesEmptyFile() throws IOException {
        Path file = tempDir.resolve("packagedefinition.txt");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        var analysis = PackageDefinitionAnalyzer.analyze(file);
        assertFalse(analysis.found());
    }

    @Test
    void notFoundForMissingFile() {
        Path missing = tempDir.resolve("does_not_exist.txt");
        var analysis = PackageDefinitionAnalyzer.analyze(missing);
        assertFalse(analysis.found());
    }

    @Test
    void handlesFileWithNoSectionMarkers() throws IOException {
        Path file = tempDir.resolve("packagedefinition.txt");
        Files.writeString(file, """
            [assembly:/test.pc_packagedefinition]
            chunk0 = chunk0.pc_rp
            chunk1 = chunk1.pc_rp
            """, StandardCharsets.UTF_8);

        var analysis = PackageDefinitionAnalyzer.analyze(file);
        assertTrue(analysis.found());
        assertEquals(2, analysis.chunkCount());
        // default section name when no @SECTION encountered
        assertEquals("DEFAULT", analysis.entries().get(0).section());
    }
}
