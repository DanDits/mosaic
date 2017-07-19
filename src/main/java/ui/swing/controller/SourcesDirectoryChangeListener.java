package ui.swing.controller;

/**
 * Created by dd on 19.07.17.
 */
public interface SourcesDirectoryChangeListener {
    void onSourcesDirectoryCreated(SourcesDirectory directory, int index);
    void onSourcesDirectoryStatusChange(SourcesDirectory directory, int index);
    void onSourcesDirectoryRemoved(SourcesDirectory directory, int previousIndex);
}
