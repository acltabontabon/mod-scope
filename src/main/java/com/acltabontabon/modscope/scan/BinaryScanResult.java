package com.acltabontabon.modscope.scan;

import java.util.List;

public record BinaryScanResult(
    List<BinaryStringHint> allHints,
    int filesScanned,
    int filesSkipped,
    BinaryScanPolicy policy
) {
    public static BinaryScanResult empty(BinaryScanPolicy policy) {
        return new BinaryScanResult(List.of(), 0, 0, policy);
    }

    public List<BinaryStringHint> usefulHints() {
        return allHints.stream().filter(BinaryStringHint::isUseful).toList();
    }

    public List<BinaryStringHint> highHints() {
        return allHints.stream().filter(h -> h.relevance() == BinaryHintRelevance.HIGH).toList();
    }

    public List<BinaryStringHint> mediumHints() {
        return allHints.stream().filter(h -> h.relevance() == BinaryHintRelevance.MEDIUM).toList();
    }

    public List<BinaryStringHint> lowHints() {
        return allHints.stream().filter(h -> h.relevance() == BinaryHintRelevance.LOW).toList();
    }

    public List<BinaryStringHint> noisyHints() {
        return allHints.stream().filter(BinaryStringHint::isSuppressed).toList();
    }

    public int usefulCount() {
        return (int) allHints.stream().filter(BinaryStringHint::isUseful).count();
    }

    public int suppressedCount() {
        return (int) allHints.stream().filter(BinaryStringHint::isSuppressed).count();
    }
}
