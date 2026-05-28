package com.acltabontabon.modscope.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory representation of {@code ~/.modscope/state/scan-history.json}. */
public final class ScanHistory {

    private final Map<String, ScanHistoryEntry> byId = new LinkedHashMap<>();

    public void upsert(ScanHistoryEntry entry) {
        byId.put(entry.gameId(), entry);
    }

    public Optional<ScanHistoryEntry> find(String gameId) {
        return Optional.ofNullable(byId.get(gameId));
    }

    public List<ScanHistoryEntry> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    public int size() { return byId.size(); }
}
