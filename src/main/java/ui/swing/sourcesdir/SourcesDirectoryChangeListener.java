package ui.swing.sourcesdir;

/**
 * Created by dd on 19.07.17.
 */
public interface SourcesDirectoryChangeListener {
    void onSourcesDirectoryCreated(SourcesDirectory directory);
    void onSourcesDirectoryStatusChange(SourcesDirectory directory);
    void onSourcesDirectoryRemoved(SourcesDirectory directory);
}
