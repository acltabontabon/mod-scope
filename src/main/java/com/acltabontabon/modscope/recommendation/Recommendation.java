package com.acltabontabon.modscope.recommendation;

import java.util.List;

public record Recommendation(
    RecommendationType type,
    String title,
    String reason,
    int confidence,
    String suggestedAction,
    RiskLevel riskLevel,
    List<String> relatedPaths
) {}
