package com.acltabontabon.modscope.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppPathsTest {

    @Test
    void safeGameIdSlugifiesAndLowercases() {
        assertEquals("007-first-light", AppPaths.safeGameId("007: First Light!", "3768760"));
        assertEquals("cyberpunk-2077", AppPaths.safeGameId("Cyberpunk 2077", "1091500"));
    }

    @Test
    void safeGameIdFallsBackToAppId() {
        assertEquals("steam-12345", AppPaths.safeGameId("!!!", "12345"));
        assertEquals("steam-12345", AppPaths.safeGameId("", "12345"));
    }

    @Test
    void stateAndReportsDirsAreUnderRoot() {
        assertTrue(AppPaths.stateDir().startsWith(AppPaths.root()));
        assertTrue(AppPaths.reportsRoot().startsWith(AppPaths.root()));
        assertTrue(AppPaths.reportsForGame("foo").startsWith(AppPaths.reportsRoot()));
        assertTrue(AppPaths.librarySummaryPath().startsWith(AppPaths.reportsRoot()));
    }
}
