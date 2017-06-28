package ui;

import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.mosaic.MosaicTile;
import data.storage.JSONStorage;
import org.pmw.tinylog.Logger;
import util.image.ColorAnalysisUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicAnalyzer {

    private FileMosaicAnalyzer() {}

    public static Set<MosaicTile<String>> analyze(Collection<MosaicTile<String>> prevTiles, File analyzeDirectory, File savePath) {
        if (!analyzeDirectory.isDirectory()) {
            throw new IllegalArgumentException("No directory:" + analyzeDirectory);
        }
        if (savePath.isDirectory()) {
            throw new IllegalArgumentException("Cannot overwrite directory to save files:" + savePath);
        }
        Set<MosaicTile<String>> tiles;
        try {
            tiles = traverseDirectory(analyzeDirectory);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed traversing directory:" + analyzeDirectory);
        }
        if (prevTiles != null) {
            tiles.addAll(prevTiles);
        }
        if (saveTiles(tiles, savePath)) {
            return tiles;
        } else {
            throw new IllegalArgumentException("Failed saving " + tiles.size() + " tiles to path:" + savePath);
        }
    }

    private static boolean saveTiles(Set<MosaicTile<String>> tiles, File savePath) {
        JSONStorage<String> storage = new JSONStorage<>();
        return storage.saveToJSON(savePath, tiles);
    }


    private static Set<MosaicTile<String>> traverseDirectory(File directory) throws IOException {
        Path path = directory.toPath();
        EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);
        ImageVisitor visitor = new ImageVisitor();
        Files.walkFileTree(path, opts, Integer.MAX_VALUE, visitor);
        return visitor.tiles;
    }

    private static class ImageVisitor extends SimpleFileVisitor<Path> {
        private Set<MosaicTile<String>> tiles = new HashSet<>();

        @Override
        public FileVisitResult visitFile(Path path,
                                         BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                try {
                    File file = path.toFile();
                    AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(file).createBitmap();
                    if (bitmap != null) {
                        int color = ColorAnalysisUtil.getAverageColor(bitmap);
                        tiles.add(new FileMosaicTile(file.getCanonicalPath(), color, bitmap.getWidth(), bitmap.getHeight()));
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
