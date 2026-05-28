package com.acltabontabon.modscope.tui;

import com.acltabontabon.modscope.core.ScanService;
import com.acltabontabon.modscope.history.ScanHistoryStore;
import com.acltabontabon.modscope.library.DetectedGameRegistry;
import com.acltabontabon.modscope.settings.AppSettingsStore;
import com.acltabontabon.modscope.triage.QuickTriageService;
import com.acltabontabon.modscope.tui.screens.GameDetailsScreen;
import com.acltabontabon.modscope.tui.screens.HomeScreen;
import com.acltabontabon.modscope.tui.screens.RecommendationScreen;
import com.acltabontabon.modscope.tui.screens.ReportViewerScreen;
import com.acltabontabon.modscope.tui.screens.ScanProgressScreen;
import com.acltabontabon.modscope.tui.screens.ScanResultsScreen;
import com.acltabontabon.modscope.tui.screens.ScanSetupScreen;
import com.acltabontabon.modscope.tui.screens.SettingsScreen;
import dev.tamboui.backend.panama.PanamaBackend;
import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ModScopeTuiApp implements ApplicationRunner {

    private final ScanService scanService;
    private final QuickTriageService quickTriageService;

    public ModScopeTuiApp(ScanService scanService, QuickTriageService quickTriageService) {
        this.scanService = scanService;
        this.quickTriageService = quickTriageService;
    }

    private static Backend createBackend() throws Exception {
        var backend = new PanamaBackend();
        if (System.getProperty("os.name", "").toLowerCase().startsWith("windows")) {
            return new WindowsPeekFixingBackend(backend);
        }
        return backend;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TuiState state = new TuiState();
        state.scanService = scanService;
        state.quickTriageService = quickTriageService;
        state.detectedGames = DetectedGameRegistry.detectAll();
        state.scanHistory = new ScanHistoryStore().load();
        state.settings = new AppSettingsStore().load();

        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(250))
            .backend(createBackend())
            .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                (event, runner) -> {
                    if (event instanceof TickEvent) {
                        // Repaint while a scan or scan-all-quick is in flight.
                        return state.screen == TuiScreen.SCAN_PROGRESS || state.scanAllQuickActive;
                    }
                    if (!(event instanceof KeyEvent keyEvent)) return false;
                    return switch (state.screen) {
                        case HOME            -> HomeScreen.handleKey(keyEvent, runner, state);
                        case GAME_DETAILS    -> GameDetailsScreen.handleKey(keyEvent, runner, state);
                        case RECOMMENDATIONS -> RecommendationScreen.handleKey(keyEvent, runner, state);
                        case REPORT_VIEWER   -> ReportViewerScreen.handleKey(keyEvent, runner, state);
                        case SETTINGS        -> SettingsScreen.handleKey(keyEvent, runner, state);
                        case SCAN_SETUP      -> ScanSetupScreen.handleKey(keyEvent, runner, state);
                        case SCAN_PROGRESS   -> ScanProgressScreen.handleKey(keyEvent, runner, state);
                        case SCAN_RESULTS    -> ScanResultsScreen.handleKey(keyEvent, runner, state);
                    };
                },
                frame -> {
                    switch (state.screen) {
                        case HOME            -> HomeScreen.render(frame, state);
                        case GAME_DETAILS    -> GameDetailsScreen.render(frame, state);
                        case RECOMMENDATIONS -> RecommendationScreen.render(frame, state);
                        case REPORT_VIEWER   -> ReportViewerScreen.render(frame, state);
                        case SETTINGS        -> SettingsScreen.render(frame, state);
                        case SCAN_SETUP      -> ScanSetupScreen.render(frame, state);
                        case SCAN_PROGRESS   -> ScanProgressScreen.render(frame, state);
                        case SCAN_RESULTS    -> ScanResultsScreen.render(frame, state);
                    }
                }
            );
        }
    }
}
