package ui.swing.controller;

import data.image.AbstractBitmap;
import util.PercentProgressListener;
import util.ProgressCallback;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Created by dd on 11.07.17.
 */
public class EasyMosaicController {
    private List<SourcesDirectory> sourcesDirectories = new ArrayList<>();
    private Set<SourcesDirectoryChangeListener> sourcesListeners = new HashSet<>();
    private File analyzedCacheDir = new File(".analyzedCache");

    public void setNoDuplicates(boolean noDuplicates) {
        this.noDuplicates = noDuplicates;
        System.out.println("No duplicates=" + noDuplicates);
    }

    public void setMatcherPreference(MatcherOption matcherPreference) {
        this.matcherPreference = matcherPreference;
        System.out.println("Preference=" + matcherPreference);
    }

    public void cancelLoadDirectory(int index) {
        SourcesDirectory curr = sourcesDirectories.get(index);
        if (!curr.isLoadingCancelled()) {
            curr.setLoadingCancelled();
            removeSourcesDirectory(index);
        }
    }

    public void loadDirectory(int index, PercentProgressListener listener) {
        SourcesDirectory directory = sourcesDirectories.get(index);
        directory.startLoading(new ProgressCallback() {
            @Override
            public boolean isCancelled() {
                return directory.isLoadingCancelled();
            }

            @Override
            public void onProgressUpdate(int progress) {
                listener.onProgressUpdate(progress);
            }
        }, () -> notifySourcesListeners(directory,
                                        (sourcesDirectoriesChangeListener, dir)
                                                -> sourcesDirectoriesChangeListener.onSourcesDirectoryStatusChange(dir, sourcesDirectories.indexOf(dir))));
    }

    public void chooseSourcesDirectories(File[] sourceFiles, SourcesDirectoryChangeListener listener) {
        Objects.requireNonNull(listener);
        if (Objects.isNull(sourceFiles) || sourceFiles.length == 0) {
            return;
        }
        sourcesListeners.add(listener);
        for (File file : sourceFiles) {
            SourcesDirectory curr = new SourcesDirectory(file, analyzedCacheDir);
            if (!sourcesDirectories.contains(curr)) {
                sourcesDirectories.add(curr);
                notifySourcesListeners(curr, (sourcesDirectoryChangeListener, directory)
                        -> sourcesDirectoryChangeListener.onSourcesDirectoryCreated(directory,
                                                                                    sourcesDirectories.indexOf(curr)));
            }
        }
    }

    private void notifySourcesListeners(SourcesDirectory directory,
                                        BiConsumer<SourcesDirectoryChangeListener, SourcesDirectory> consumer) {
        sourcesListeners.forEach(listener -> consumer.accept(listener, directory));
    }

    public void setSourcesDirectoryActive(int index, boolean active) {
        SourcesDirectory curr = sourcesDirectories.get(index);
        System.out.println("Old active=" + curr.isActive() + " new = " + active);
        if (curr.isActive() != active) {
            curr.setActive(active);
            notifySourcesListeners(curr, (sourcesDirectoryChangeListener, directory)
                    -> sourcesDirectoryChangeListener.onSourcesDirectoryStatusChange(directory, index));
        }
    }

    public void removeSourcesDirectory(int index) {
        SourcesDirectory curr = sourcesDirectories.remove(index);
        notifySourcesListeners(curr, (sourcesDirectoryChangeListener, directory)
                -> sourcesDirectoryChangeListener.onSourcesDirectoryRemoved(directory, index));
        if (sourcesDirectories.isEmpty()) {
            sourcesListeners.clear();
        }
    }

    public enum MatcherOption {
        RANDOM, COLOR, RESOLUTION
    }

    private enum EffectOption {
        LEGO, SIMPLE_RECTS, RECTS
    }
    private File originalFile;
    private AbstractBitmap original;

    public boolean isNoDuplicates() {
        return noDuplicates;
    }

    public MatcherOption getMatcherPreference() {
        return matcherPreference;
    }

    private boolean noDuplicates = true;
    private MatcherOption matcherPreference = MatcherOption.COLOR;
    private File tilesDirectory; // TODO system images directory
}
