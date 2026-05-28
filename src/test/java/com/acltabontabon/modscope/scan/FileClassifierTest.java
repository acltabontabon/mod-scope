package com.acltabontabon.modscope.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileClassifierTest {

    @Test
    void classifiesExecutable() {
        assertEquals(FileCategory.EXECUTABLE, FileClassifier.classify("exe", 1024));
        assertEquals(FileCategory.EXECUTABLE, FileClassifier.classify("dll", 512 * 1024));
    }

    @Test
    void classifiesArchive() {
        assertEquals(FileCategory.ARCHIVE, FileClassifier.classify("pak", 100 * 1024 * 1024));
        assertEquals(FileCategory.ARCHIVE, FileClassifier.classify("zip", 1024));
    }

    @Test
    void classifiesConfig() {
        assertEquals(FileCategory.CONFIG, FileClassifier.classify("ini", 512));
        assertEquals(FileCategory.CONFIG, FileClassifier.classify("cfg", 1024));
    }

    @Test
    void classifiesVideo() {
        assertEquals(FileCategory.VIDEO, FileClassifier.classify("bik", 50 * 1024 * 1024));
        assertEquals(FileCategory.VIDEO, FileClassifier.classify("mp4", 1024));
    }

    @Test
    void classifiesText() {
        assertEquals(FileCategory.TEXT, FileClassifier.classify("json", 1024));
        assertEquals(FileCategory.TEXT, FileClassifier.classify("xml", 1024));
    }

    @Test
    void classifiesUnknownLargeForHugeFiles() {
        long huge = 600L * 1024 * 1024;
        assertEquals(FileCategory.UNKNOWN_LARGE, FileClassifier.classify("dat2", huge));
    }

    @Test
    void classifiesOtherForSmallUnknownFiles() {
        assertEquals(FileCategory.OTHER, FileClassifier.classify("xyz", 1024));
    }

    @Test
    void isTextReadableForKnownTextExtensions() {
        assertTrue(FileClassifier.isTextReadable("cfg"));
        assertTrue(FileClassifier.isTextReadable("json"));
        assertTrue(FileClassifier.isTextReadable("ini"));
        assertFalse(FileClassifier.isTextReadable("pak"));
        assertFalse(FileClassifier.isTextReadable("exe"));
    }
}
