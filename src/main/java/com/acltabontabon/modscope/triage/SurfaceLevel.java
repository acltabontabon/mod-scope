package com.acltabontabon.modscope.triage;

public enum SurfaceLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    public boolean atLeast(SurfaceLevel other) {
        return ordinal() >= other.ordinal();
    }
}
