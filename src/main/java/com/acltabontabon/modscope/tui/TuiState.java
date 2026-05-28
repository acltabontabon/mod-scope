package com.acltabontabon.modscope.tui;

import com.acltabontabon.modscope.core.ScanResult;
import com.acltabontabon.modscope.core.ScanService;
import com.acltabontabon.modscope.history.ScanHistory;
import com.acltabontabon.modscope.library.DetectedGame;
import com.acltabontabon.modscope.recommendation.Recommendation;
import com.acltabontabon.modscope.settings.AppSettings;
import com.acltabontabon.modscope.storage.AppPaths;
import com.acltabontabon.modscope.steam.SteamAppManifest;
import com.acltabontabon.modscope.triage.GameTriageResult;
import com.acltabontabon.modscope.triage.QuickTriageService;
import dev.tamboui.widgets.list.ListState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TuiState {

    public TuiScreen screen = TuiScreen.HOME;

    // Home dashboard
    public final ListState homeList = new ListState();
    public List<DetectedGame> detectedGames = List.of();
    /** Legacy mirror so the existing HomeScreen renderer keeps working. */
    public List<SteamAppManifest> detectedManifests = List.of();
    public final Map<String, GameTriageResult> triageCache = new HashMap<>();
    public String filterQuery = "";

    // Game details
    public DetectedGame selectedGame = null;
    public final ListState gameDetailsList = new ListState();

    // Recommendations view
    public List<Recommendation> currentRecommendations = List.of();
    public final ListState recommendationsList = new ListState();

    // Report viewer
    public java.nio.file.Path reportViewerPath = null;
    public List<String> reportViewerLines = List.of();
    public int reportViewerScroll = 0;

    // Settings
    public AppSettings settings = AppSettings.defaults();
    public final ListState settingsList = new ListState();

    // Scan history (loaded at startup, refreshed after each triage/scan)
    public ScanHistory scanHistory = new ScanHistory();

    // Scan setup
    public final ListState setupList = new ListState();
    public String setupProfileId = null;
    public String setupGameDir = "";
    public String setupGameName = "";
    public boolean setupDeep = false;
    public String setupOutputDir = AppPaths.reportsRoot().toString();

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

    // Multi-game (scan-all-quick) state
    public volatile boolean scanAllQuickActive = false;
    public volatile String scanAllCurrentGame = "";
    public volatile int scanAllCompleted = 0;
    public volatile int scanAllTotal = 0;
    public final List<String> scanAllFailures = new ArrayList<>();

    // Scan services
    public ScanService scanService;
    public QuickTriageService quickTriageService;

    // Scan results
    public volatile ScanResult scanResult = null;
    public volatile Exception scanError = null;
    public final ListState resultsLeadsList = new ListState();

    public TuiState() {
        homeList.select(0);
        setupList.select(0);
        resultsLeadsList.select(0);
        gameDetailsList.select(0);
        recommendationsList.select(0);
        settingsList.select(0);
    }
}
