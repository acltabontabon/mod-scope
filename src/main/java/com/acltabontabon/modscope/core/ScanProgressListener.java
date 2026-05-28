package com.acltabontabon.modscope.core;

public interface ScanProgressListener {
    void onPhaseStarted(String phase);
    void onProgress(int filesScanned, int configLike, int archives, int videos, int hintsFound);
    void onLog(String message);
    void onComplete(ScanResult result);
    void onError(Exception error);

    static ScanProgressListener silent() {
        return new ScanProgressListener() {
            @Override public void onPhaseStarted(String phase) {}
            @Override public void onProgress(int a, int b, int c, int d, int e) {}
            @Override public void onLog(String message) {}
            @Override public void onComplete(ScanResult result) {}
            @Override public void onError(Exception error) {}
        };
    }
}
