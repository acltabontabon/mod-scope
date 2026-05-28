package com.acltabontabon.modscope.tui.screens;

import com.acltabontabon.modscope.core.ScanMode;
import com.acltabontabon.modscope.settings.AppSettings;
import com.acltabontabon.modscope.settings.AppSettingsStore;
import com.acltabontabon.modscope.tui.TuiScreen;
import com.acltabontabon.modscope.tui.TuiState;
import com.acltabontabon.modscope.tui.components.StatusPanel;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListWidget;

public final class SettingsScreen {

    private SettingsScreen() {}

    public static boolean handleKey(KeyEvent event, TuiRunner runner, TuiState state) {
        String[] items = buildItems(state);
        return switch (event) {
            case KeyEvent k when k.isDown()   -> { state.settingsList.selectNext(items.length); yield true; }
            case KeyEvent k when k.isUp()     -> { state.settingsList.selectPrevious(); yield true; }
            case KeyEvent k when k.isSelect() -> { toggle(state); yield true; }
            case KeyEvent k when k.isCancel() -> { state.screen = TuiScreen.HOME; yield true; }
            case KeyEvent k when k.isQuit()   -> { runner.quit(); yield false; }
            default -> false;
        };
    }

    private static void toggle(TuiState state) {
        Integer sel = state.settingsList.selected();
        if (sel == null) return;
        AppSettings s = state.settings;
        AppSettings updated = switch (sel) {
            case 0 -> new AppSettings(s.reportsDir(), nextMode(s.defaultScanMode()),
                s.includeBinaryStringScanByDefault(), s.includeVendorRuntimeLibs(),
                s.includeGameExecutableStrings(), s.includeLargeArchiveSampling(),
                s.maxStringSampleMb(), s.maxHashSizeMb(), s.showAdvancedWarnings());
            case 1 -> flip(s, 1);
            case 2 -> flip(s, 2);
            case 3 -> flip(s, 3);
            case 4 -> flip(s, 4);
            case 5 -> flip(s, 5);
            default -> s;
        };
        state.settings = updated;
        try { new AppSettingsStore().save(updated); } catch (java.io.IOException ignored) {}
    }

    private static AppSettings flip(AppSettings s, int idx) {
        return switch (idx) {
            case 1 -> new AppSettings(s.reportsDir(), s.defaultScanMode(),
                !s.includeBinaryStringScanByDefault(), s.includeVendorRuntimeLibs(),
                s.includeGameExecutableStrings(), s.includeLargeArchiveSampling(),
                s.maxStringSampleMb(), s.maxHashSizeMb(), s.showAdvancedWarnings());
            case 2 -> new AppSettings(s.reportsDir(), s.defaultScanMode(),
                s.includeBinaryStringScanByDefault(), !s.includeVendorRuntimeLibs(),
                s.includeGameExecutableStrings(), s.includeLargeArchiveSampling(),
                s.maxStringSampleMb(), s.maxHashSizeMb(), s.showAdvancedWarnings());
            case 3 -> new AppSettings(s.reportsDir(), s.defaultScanMode(),
                s.includeBinaryStringScanByDefault(), s.includeVendorRuntimeLibs(),
                !s.includeGameExecutableStrings(), s.includeLargeArchiveSampling(),
                s.maxStringSampleMb(), s.maxHashSizeMb(), s.showAdvancedWarnings());
            case 4 -> new AppSettings(s.reportsDir(), s.defaultScanMode(),
                s.includeBinaryStringScanByDefault(), s.includeVendorRuntimeLibs(),
                s.includeGameExecutableStrings(), !s.includeLargeArchiveSampling(),
                s.maxStringSampleMb(), s.maxHashSizeMb(), s.showAdvancedWarnings());
            case 5 -> new AppSettings(s.reportsDir(), s.defaultScanMode(),
                s.includeBinaryStringScanByDefault(), s.includeVendorRuntimeLibs(),
                s.includeGameExecutableStrings(), s.includeLargeArchiveSampling(),
                s.maxStringSampleMb(), s.maxHashSizeMb(), !s.showAdvancedWarnings());
            default -> s;
        };
    }

    private static ScanMode nextMode(ScanMode m) {
        return switch (m) {
            case QUICK -> ScanMode.STANDARD;
            case STANDARD -> ScanMode.DEEP;
            case DEEP -> ScanMode.QUICK;
        };
    }

    private static String[] buildItems(TuiState state) {
        AppSettings s = state.settings;
        return new String[] {
            "Default scan mode:               " + s.defaultScanMode(),
            "Binary string scan by default:   " + (s.includeBinaryStringScanByDefault() ? "ON" : "OFF"),
            "Include vendor/runtime libs:     " + (s.includeVendorRuntimeLibs() ? "ON" : "OFF"),
            "Include game executable strings: " + (s.includeGameExecutableStrings() ? "ON" : "OFF"),
            "Include large archive sampling:  " + (s.includeLargeArchiveSampling() ? "ON" : "OFF"),
            "Show advanced warnings:          " + (s.showAdvancedWarnings() ? "ON" : "OFF"),
            "",
            "Reports dir:                     " + s.reportsDir(),
            "Max string sample MB:            " + s.maxStringSampleMb(),
            "Max hash size MB:                " + s.maxHashSizeMb()
        };
    }

    public static void render(Frame frame, TuiState state) {
        Rect area = frame.area();
        Block outer = Block.builder()
            .title(Title.from(" Settings "))
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build();
        Rect inner = outer.inner(area);
        frame.renderWidget(outer, area);

        int listHeight = Math.max(1, inner.height() - 2);
        Rect listArea = Rect.of(new Position(inner.x(), inner.y()), new Size(inner.width(), listHeight));
        frame.renderStatefulWidget(
            ListWidget.builder()
                .items(buildItems(state))
                .highlightStyle(Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD))
                .highlightSymbol("▶ ")
                .build(),
            listArea,
            state.settingsList
        );

        StatusPanel.render(frame, inner, "↑↓ navigate   Enter toggle   Esc back");
    }
}
