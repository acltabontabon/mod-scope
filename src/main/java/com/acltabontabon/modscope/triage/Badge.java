package com.acltabontabon.modscope.triage;

/**
 * Short, user-facing chips that summarize a game's modding situation.
 * Rendered with fixed colors by {@code tui/components/BadgeBar}.
 */
public enum Badge {
    LOOSE_FILES("Loose files"),
    ARCHIVE_HEAVY("Archive-heavy"),
    CONFIGS_FOUND("Configs found"),
    SAVES_FOUND("Saves found"),
    EXTERNAL_TOOL_NEEDED("External tool needed"),
    UNKNOWN_ENGINE("Unknown engine"),
    HIGH_CONFIDENCE("High confidence"),
    LOW_CONFIDENCE("Low confidence"),
    GOOD_FIRST_TARGET("Good first target"),
    HARD_TARGET("Hard target");

    private final String label;

    Badge(String label) { this.label = label; }

    public String label() { return label; }
}
