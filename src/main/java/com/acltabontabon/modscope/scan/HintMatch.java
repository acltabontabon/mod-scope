package com.acltabontabon.modscope.scan;

public record HintMatch(
    String filePath,
    int lineNumber,
    String keyword,
    String snippet,
    Confidence confidence
) {
    public enum Confidence { HIGH, MEDIUM, LOW }
}
