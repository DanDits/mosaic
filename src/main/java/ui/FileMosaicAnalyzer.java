package ui;

import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.storage.JSONStorage;
import data.storage.MosaicTile;
import org.pmw.tinylog.Logger;
import util.ProgressCallback;
import util.image.ColorAnalysisUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicAnalyzer {

    private FileMosaicAnalyzer() {}

    public static Set<MosaicTile<String>> analyze(Collection<MosaicTile<String>> prevTiles, File analyzeDirectory,
                                                  File savePath, ProgressCallback updateCallback) {
        if (!analyzeDirectory.isDirectory()) {
            throw new IllegalArgumentException("No directory:" + analyzeDirectory);
        }
        if (savePath.isDirectory()) {
            throw new IllegalArgumentException("Cannot overwrite directory to save files:" + savePath);
        }
        int estimatedFilesCount = -1;
        if (updateCallback != null) {
            estimatedFilesCount = FileMosaicAnalyzer.estimateContainedImageFiles(analyzeDirectory, updateCallback);
        }
        ImageVisitor visitor = new ImageVisitor(updateCallback, estimatedFilesCount);
        try {
            traverseDirectory(analyzeDirectory, visitor);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed traversing directory:" + analyzeDirectory);
        }
        if (prevTiles != null) {
            visitor.tiles.addAll(prevTiles);
        }
        if (saveTiles(visitor.tiles, savePath)) {
            return visitor.tiles;
        } else {
            throw new IllegalArgumentException("Failed saving " + visitor.tiles.size() + " tiles to path:" + savePath);
        }
    }

    public static int estimateContainedImageFiles(File analyzeDirectory, ProgressCallback updateCallback) {
        ImageFileCountEstimationVisitor visitor = new ImageFileCountEstimationVisitor(updateCallback);
        try {
            traverseDirectory(analyzeDirectory, visitor);
        } catch (IOException e) {
            return -1;
        }
        return visitor.fileCount;
    }

    private static boolean saveTiles(Set<MosaicTile<String>> tiles, File savePath) {
        JSONStorage<String> storage = new JSONStorage<>();
        return storage.saveToJSON(savePath, tiles);
    }


    private static <V extends SimpleFileVisitor<Path>> void traverseDirectory(File directory, V visitor)
            throws IOException {
        Path path = directory.toPath();
        EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);
        Files.walkFileTree(path, opts, Integer.MAX_VALUE, visitor);
    }

    private static class ImageFileCountEstimationVisitor extends SimpleFileVisitor<Path> {
        private final ProgressCallback updateCallback;
        private List<String> searchExtensions = Arrays.asList("png", "jpg", "gif", "jpeg");

        private int fileCount;

        public ImageFileCountEstimationVisitor(ProgressCallback updateCallback) {
            this.updateCallback = updateCallback;
        }

        @Override
        public FileVisitResult visitFile(Path path,
                                         BasicFileAttributes attr) {
            if (updateCallback != null && updateCallback.isCancelled()) {
                return FileVisitResult.TERMINATE;
            }
            if (attr.isRegularFile()) {
                File file = path.toFile();
                String fileName = file.getName().toLowerCase();
                for (String extension : searchExtensions) {
                    if (fileName.endsWith(extension)) {
                        fileCount++;
                        break;
                    }
                }
            }
            return CONTINUE;
        }
    }

    private static class ImageVisitor extends SimpleFileVisitor<Path> {
        private final int estimatedFilesCount;
        private final ProgressCallback updateCallback;
        private Set<MosaicTile<String>> tiles = new HashSet<>();
        private int count;

        public ImageVisitor(ProgressCallback updater, int estimatedFilesCount) {
            this.updateCallback = updater;
            this.estimatedFilesCount = estimatedFilesCount;
        }

        @Override
        public FileVisitResult visitFile(Path path,
                                         BasicFileAttributes attr) {
            if (updateCallback != null && updateCallback.isCancelled()) {
                return FileVisitResult.TERMINATE;
            }
            if (attr.isRegularFile()) {
                try {
                    File file = path.toFile();
                    AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(file).createBitmap();
                    if (bitmap != null) {
                        int color = ColorAnalysisUtil.getAverageColor(bitmap);
                        tiles.add(new FileMosaicTile(file.getCanonicalPath(), color, bitmap.getWidth(), bitmap.getHeight()));
                        if (updateCallback != null && estimatedFilesCount > 0) {
                            count++;
                            updateCallback.onProgressUpdate((int) (100 * count / (double) estimatedFilesCount));
                        }
                    } // else not an image file
                } catch (IOException e) {
                    Logger.error("Could not read image file {}", e);
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                        IOException exc) {
            if (exc instanceof FileSystemLoopException) {
                Logger.error("Detected cycle when analyzing files: {}", file);
            } else {
                Logger.error("Could not visit file {}: {}", file, exc);
            }
            return CONTINUE;
        }
    }
}
