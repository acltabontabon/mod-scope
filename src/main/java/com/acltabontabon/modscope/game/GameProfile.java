package com.acltabontabon.modscope.game;

import java.util.List;

public record GameProfile(
    String id,
    String displayName,
    String steamAppId,
    List<String> likelyFolderNames,
    List<String> executableCandidates,
    List<String> inspectSubfolders,
    List<String> savePathTemplates
) {}
