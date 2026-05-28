package com.acltabontabon.modscope.scan;

public record BinaryStringHint(
    String relativePath,
    FileCategory sourceCategory,
    long fileOffset,
    String keyword,
    String context,
    BinaryHintRelevance relevance,
    String suppressionReason,
    String confidenceExplanation
) {
    public boolean isUseful() {
        return relevance != BinaryHintRelevance.NOISE;
    }

    public boolean isSuppressed() {
        return relevance == BinaryHintRelevance.NOISE;
    }
}
