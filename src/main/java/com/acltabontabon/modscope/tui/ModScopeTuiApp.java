package com.acltabontabon.modscope.tui;

import com.acltabontabon.modscope.core.ScanService;
import com.acltabontabon.modscope.util.WindowsConsoleInit;
import com.acltabontabon.modscope.tui.screens.HomeScreen;
import com.acltabontabon.modscope.tui.screens.ScanProgressScreen;
import com.acltabontabon.modscope.tui.screens.ScanResultsScreen;
import com.acltabontabon.modscope.tui.screens.ScanSetupScreen;
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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Enable VT processing + UTF-8 on the Windows console before JLine initialises,
        // so JLine detects a VT-capable terminal and doesn't fall back to dumb mode.
        WindowsConsoleInit.apply();
        System.setProperty("org.jline.terminal.provider", "ffm");

        TuiState state = new TuiState();
        state.scanService = scanService;

        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(250))
            .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                (event, runner) -> {
                    // Drive re-renders during scan without needing key presses
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
