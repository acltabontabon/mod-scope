package com.acltabontabon.modscope.core;

import com.acltabontabon.modscope.game.GameInstall;
import com.acltabontabon.modscope.game.GameInstallDetector;
import com.acltabontabon.modscope.game.GameProfile;
import com.acltabontabon.modscope.game.GameProfileRegistry;
import com.acltabontabon.modscope.report.ReportWriter;
import com.acltabontabon.modscope.save.SaveCandidate;
import com.acltabontabon.modscope.save.SaveLocator;
import com.acltabontabon.modscope.scan.FileCategory;
import com.acltabontabon.modscope.scan.FileEntry;
import com.acltabontabon.modscope.scan.FileInventoryScanner;
import com.acltabontabon.modscope.scan.HintMatch;
import com.acltabontabon.modscope.scan.TextHintScanner;
import com.acltabontabon.modscope.util.FileSizeFormatter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ScanService {

    public ScanResult scan(ScanOptions options, ScanProgressListener listener) throws IOException {
        final ScanProgressListener progress = listener != null ? listener : ScanProgressListener.silent();

        String scannedAt = Instant.now().toString();

        progress.onPhaseStarted("Detecting game");
        progress.onLog("Looking for game installation...");
        Optional<GameInstall> install = detectInstall(options);

        if (install.isEmpty() && options.gameDir() == null) {
            throw new IOException("Game installation not found. Specify the path in Scan Setup.");
        }

        install.ifPresentOrElse(
            i -> progress.onLog("+ Found: " + i.profile().displayName() + " at " + i.installPath()),
            () -> progress.onLog("~ Game not found via Steam, using manual path")
        );

        GameProfile profile = install
            .map(GameInstall::profile)
            .orElseGet(() -> resolveProfile(options));

        progress.onPhaseStarted("Locating saves");
        progress.onLog("Searching Steam userdata for save candidates...");
        List<SaveCandidate> saves = profile != null ? SaveLocator.locate(profile) : List.of();
        if (saves.isEmpty()) {
            progress.onLog("~ No save candidates found");
        } else {
            for (SaveCandidate save : saves) {
                String status = save.exists()
                    ? "+ exists (" + FileSizeFormatter.format(save.sizeBytes()) + ")"
                    : "  not found";
                progress.onLog(status + " — " + save.path().getFileName());
            }
        }

        progress.onPhaseStarted("Inventorying files");
        Path scanDir = install.map(GameInstall::installPath).orElse(options.gameDir());
        progress.onLog("Walking: " + scanDir);

        List<FileEntry> files = new ArrayList<>();
        AtomicInteger configLike = new AtomicInteger();
        AtomicInteger archives = new AtomicInteger();
        AtomicInteger videos = new AtomicInteger();
        int[] total = {0};
        int[] lastLoggedMilestone = {0};

        if (scanDir != null) {
            files.addAll(FileInventoryScanner.scan(scanDir, options.mode(), entry -> {
                total[0]++;
                if (entry.category() == FileCategory.CONFIG || entry.category() == FileCategory.TEXT) configLike.incrementAndGet();
                if (entry.category() == FileCategory.ARCHIVE) archives.incrementAndGet();
                if (entry.category() == FileCategory.VIDEO) videos.incrementAndGet();

                int milestone = (total[0] / 100) * 100;
                if (milestone > lastLoggedMilestone[0]) {
                    lastLoggedMilestone[0] = milestone;
                    progress.onLog("  " + total[0] + " files scanned — "
                        + configLike.get() + " configs, "
                        + archives.get() + " archives, "
                        + videos.get() + " videos");
                }
                progress.onProgress(total[0], configLike.get(), archives.get(), videos.get(), 0);
            }));
        }
        progress.onLog("+ Inventory complete: " + files.size() + " files total");

        progress.onPhaseStarted("Scanning text/config hints");
        progress.onLog("Searching for QoL keywords in readable files...");
        List<HintMatch> hints = new ArrayList<>();
        for (FileEntry entry : files) {
            if (scanDir != null) {
                String sep = String.valueOf(scanDir.getFileSystem().getSeparator().charAt(0));
                List<HintMatch> fileHints = TextHintScanner.scan(
                    scanDir.resolve(entry.relativePath().replace('/', sep.charAt(0))),
                    entry.extension()
                );
                if (!fileHints.isEmpty()) {
                    hints.addAll(fileHints);
                    progress.onLog("* " + fileHints.size() + " hint(s) in " + entry.relativePath());
                    progress.onProgress(total[0], configLike.get(), archives.get(), videos.get(), hints.size());
                }
            }
        }
        progress.onLog("+ Hint scan complete: " + hints.size() + " match(es)");

        progress.onPhaseStarted("Writing reports");
        Path outputDir = options.outputDir();
        progress.onLog("Writing reports to " + outputDir + " ...");
        ScanResult result = new ScanResult(install, saves, files, hints, outputDir, scannedAt, options);
        ReportWriter.write(outputDir, result);
        progress.onLog("+ scan-summary.md");
        progress.onLog("+ file-inventory.json");
        progress.onLog("+ candidates.md");
        progress.onLog("+ text-hints.md");
        progress.onLog("+ save-locations.md");

        progress.onComplete(result);
        return result;
    }

    private Optional<GameInstall> detectInstall(ScanOptions options) {
        GameProfile profile = resolveProfile(options);
        if (profile == null) {
            profile = GameProfileRegistry.all().stream().findFirst().orElse(null);
        }
        if (profile == null) return Optional.empty();
        return GameInstallDetector.detect(profile, options);
    }

    private GameProfile resolveProfile(ScanOptions options) {
        if (options.profileId() != null) {
            return GameProfileRegistry.findById(options.profileId()).orElse(null);
        }
        return null;
    }
}
