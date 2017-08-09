package ui.swing.sourcesdir;

import data.storage.MosaicTile;
import ui.FileMosaicAnalyzer;
import ui.FileMosaicJSONBuilder;
import util.MultiStepProgressCallback;
import util.ProgressCallback;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Created by dd on 19.07.17.
 */
public class SourcesDirectory {
    private volatile boolean loaded;
    private final File cacheDir;
    private File file;
    private boolean active;
    private volatile Set<MosaicTile<String>> allTiles;
    private volatile boolean loadingCancelled;

    public SourcesDirectory(File file, File cacheDir) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(cacheDir);
        this.cacheDir = cacheDir;
        this.file = file;
        this.active = false;
        this.loaded = false;
    }

    public int getTilesCount() {
        if (allTiles == null) {
            return -1;
        }
        return allTiles.size();
    }

    public void setLoadingCancelled() {
        this.loadingCancelled = true;
    }

    public boolean isLoadingCancelled() {
        return loadingCancelled;
    }


    private static class UpdateJob implements Runnable, ProgressCallback {
        private int progress;
        private final ProgressCallback listener;

        private UpdateJob(ProgressCallback listener) {
            Objects.requireNonNull(listener);
            this.listener = listener;
            this.progress = 0;
        }

        @Override
        public void run() {
            listener.onProgressUpdate(progress);
        }

        @Override
        public void onProgressUpdate(int progress) {
            if (progress > this.progress) {
                this.progress = progress;
                SwingUtilities.invokeLater(this);
            }
        }

        @Override
        public boolean isCancelled() {
            return listener.isCancelled();
        }
    }

    void startLoading(ProgressCallback listener, Runnable onFinish) {
        Objects.requireNonNull(listener);
        Objects.requireNonNull(onFinish);
        if (isLoaded()) {
            throw new IllegalStateException("Directory already loaded: " + this);
        }
        MultiStepProgressCallback multiUpdate = new MultiStepProgressCallback(listener,
                                                                                     new double[] {0.1, 0.9});
        UpdateJob updater = new UpdateJob(multiUpdate);
        Thread loader = new Thread(() -> {
            loadingCancelled = false;
            final File cacheFile = getCacheFile();
            Set<MosaicTile<String>> tiles = FileMosaicJSONBuilder.loadExistingTiles(cacheFile, updater);
            multiUpdate.nextStep();
            if (updater.isCancelled()) {
                return;
            }
            FileMosaicAnalyzer analyzer = new FileMosaicAnalyzer(tiles);
            Set<MosaicTile<String>> all = analyzer.analyze(file, cacheFile, updater);
            if (updater.isCancelled()) {
                return;
            }
            allTiles = all;
            loaded = true;
            if (getTilesCount() > 0) {
                active = true;
            }
            multiUpdate.nextStep();
            onFinish.run();
        });
        loader.start();

    }

    private File getCacheFile() {
        if (cacheDir.exists() && !cacheDir.isDirectory()) {
            throw new IllegalStateException("Cache dir is a file:" + cacheDir);
        }
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new IllegalStateException("Could not create cache dir:" + cacheDir);
            }
        }
        String base;
        try{
            base = file.getCanonicalPath();
        } catch (IOException e) {
            base = file.getAbsolutePath();
        }
        String name = base.replace(File.separator, ".");
        return new File(cacheDir, name);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SourcesDirectory that = (SourcesDirectory) o;

        return file.equals(that.file);
    }

    @Override
    public String toString() {
        return "Sources:" + file.getAbsolutePath();
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    public boolean isActive() {
        return active;
    }

    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(file.getAbsolutePath());
        int tilesCount = getTilesCount();
        if (tilesCount >= 0) {
            builder.append(" (").append(tilesCount).append(")");
        }
        return builder.toString();
    }
}
