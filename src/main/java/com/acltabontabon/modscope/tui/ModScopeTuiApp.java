package com.acltabontabon.modscope.tui;

import com.acltabontabon.modscope.core.ScanService;
import com.acltabontabon.modscope.game.GameProfileRegistry;
import com.acltabontabon.modscope.steam.SteamLibraryScanner;
import com.acltabontabon.modscope.steam.SteamLocator;

import java.util.Comparator;
import java.util.List;
import com.acltabontabon.modscope.tui.screens.HomeScreen;
import com.acltabontabon.modscope.tui.screens.ScanProgressScreen;
import com.acltabontabon.modscope.tui.screens.ScanResultsScreen;
import com.acltabontabon.modscope.tui.screens.ScanSetupScreen;
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

    public ModScopeTuiApp(ScanService scanService) {
        this.scanService = scanService;
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
        state.detectedManifests = SteamLocator.findSteamRoot()
            .map(root -> SteamLibraryScanner.allManifests(SteamLibraryScanner.findLibraryFolders(root)))
            .orElse(List.of())
            .stream()
            .filter(m -> !m.name().isBlank())
            .sorted(Comparator.<com.acltabontabon.modscope.steam.SteamAppManifest, Integer>comparing(
                    m -> GameProfileRegistry.findByAppId(m.appId()).isPresent() ? 0 : 1)
                .thenComparing(m -> m.name().toLowerCase()))
            .toList();

        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(250))
            .backend(createBackend())
            .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                (event, runner) -> {
                    if (event instanceof TickEvent) {
                        return state.screen == TuiScreen.SCAN_PROGRESS;
                    }
                    if (!(event instanceof KeyEvent keyEvent)) return false;
                    return switch (state.screen) {
                        case HOME          -> HomeScreen.handleKey(keyEvent, runner, state);
                        case SCAN_SETUP    -> ScanSetupScreen.handleKey(keyEvent, runner, state);
                        case SCAN_PROGRESS -> ScanProgressScreen.handleKey(keyEvent, runner, state);
                        case SCAN_RESULTS  -> ScanResultsScreen.handleKey(keyEvent, runner, state);
                    };
                },
                frame -> {
                    switch (state.screen) {
                        case HOME          -> HomeScreen.render(frame, state);
                        case SCAN_SETUP    -> ScanSetupScreen.render(frame, state);
                        case SCAN_PROGRESS -> ScanProgressScreen.render(frame, state);
                        case SCAN_RESULTS  -> ScanResultsScreen.render(frame, state);
                    }
                }
            );
        }
    }
}
