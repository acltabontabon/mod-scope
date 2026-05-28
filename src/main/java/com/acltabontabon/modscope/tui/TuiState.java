package com.acltabontabon.modscope.tui;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.core.ScanService;
import com.acltabontabon.modscope.game.GameInstall;
import dev.tamboui.widgets.list.ListState;

import java.util.ArrayList;
import java.util.List;

public final class TuiState {

    public TuiScreen screen = TuiScreen.HOME;

    // Home screen — detected installs populated at startup
    public final ListState homeList = new ListState();
    public List<GameInstall> detectedGames = List.of();

    // Scan setup
    public final ListState setupList = new ListState();
    public String setupProfileId = "007-first-light";
    public String setupGameDir = "";
    public boolean setupDeep = false;
    public String setupOutputDir = ".modscope/reports";
    public static final String[] SETUP_PROFILES = { "007-first-light" };

    // Scan setup — binary policy toggles
    public boolean setupIncludeGameExe = false;
    public boolean setupIncludeVendorLibs = false;
    public boolean setupIncludeLargeArchives = false;

    // Scan progress
    public volatile String currentPhase = "Starting...";
    public volatile int filesScanned = 0;
    public volatile int configLike = 0;
    public volatile int archives = 0;
    public volatile int videos = 0;
    public volatile int hintsFound = 0;
    public volatile int binaryHintsTotal = 0;
    public volatile int binaryHintsUseful = 0;
    public volatile int binaryHintsSuppressed = 0;
    public volatile boolean scanStarted = false;
    public final List<String> scanLog = new ArrayList<>();

    // Scan results
    public ScanService scanService;

    public volatile ScanResult scanResult = null;
    public volatile Exception scanError = null;
    public final ListState resultsLeadsList = new ListState();

    public TuiState() {
        homeList.select(0);
        setupList.select(0);
        resultsLeadsList.select(0);
    }
}
