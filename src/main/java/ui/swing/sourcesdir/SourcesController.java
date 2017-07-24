package ui.swing.sourcesdir;

import util.PercentProgressListener;
import util.ProgressCallback;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Created by dd on 24.07.17.
 */
public class SourcesController {

    private List<SourcesDirectory> sourcesDirectories = new ArrayList<>();
    private Set<SourcesDirectoryChangeListener> sourcesListeners = new HashSet<>();
    private File analyzedCacheDir = new File(".analyzedCache");

    public void cancelLoadDirectory(SourcesDirectory directory) {
        checkDirectoryIsControlled(directory);
        if (!directory.isLoadingCancelled()) {
            directory.setLoadingCancelled();
            removeSourcesDirectory(directory);
        }
    }

    private void checkDirectoryIsControlled(SourcesDirectory directory) {
        System.err.println("Checking dirs:" + sourcesDirectories);
        if (!sourcesDirectories.contains(directory)) {
            throw new IllegalArgumentException("Directory not managed by controller.");
        }
    }

    public int getSourcesDirectoryIndex(SourcesDirectory directory) {
        checkDirectoryIsControlled(directory);
        return sourcesDirectories.indexOf(directory);
    }

    public void loadDirectory(SourcesDirectory directory, PercentProgressListener listener) {
        checkDirectoryIsControlled(directory);
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
                                        SourcesDirectoryChangeListener::onSourcesDirectoryStatusChange));
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
                notifySourcesListeners(curr, SourcesDirectoryChangeListener::onSourcesDirectoryCreated);
            }
        }
    }

    private void notifySourcesListeners(SourcesDirectory directory,
                                        BiConsumer<SourcesDirectoryChangeListener, SourcesDirectory> consumer) {
        sourcesListeners.forEach(listener -> consumer.accept(listener, directory));
    }

    public void setSourcesDirectoryActive(SourcesDirectory directory, boolean active) {
        checkDirectoryIsControlled(directory);
        System.out.println("Old active=" + directory.isActive() + " new = " + active);
        if (directory.isActive() != active) {
            directory.setActive(active);
            notifySourcesListeners(directory, SourcesDirectoryChangeListener::onSourcesDirectoryStatusChange);
        }
    }

    public void removeSourcesDirectory(SourcesDirectory directory) {
        checkDirectoryIsControlled(directory);
        notifySourcesListeners(directory, SourcesDirectoryChangeListener::onSourcesDirectoryRemoved);
        sourcesDirectories.remove(directory);
        if (sourcesDirectories.isEmpty()) {
            sourcesListeners.clear();
        }
    }
}
