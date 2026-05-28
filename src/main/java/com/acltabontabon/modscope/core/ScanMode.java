package com.acltabontabon.modscope.core;

public enum ScanMode {
    /** Fast triage: no hashing, no binary string scan, no per-file text-hint scan. */
    QUICK,
    /** Standard scan with hashing capped at 100 MB and text hints enabled. */
    STANDARD,
    /** Deep scan: unbounded hashing and full binary scanning. */
    DEEP
}
