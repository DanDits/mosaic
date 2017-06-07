package ui;

import data.*;
import matching.SimpleLinearTileMatcher;
import matching.TileMatcher;
import reconstruction.pattern.CirclePatternReconstructor;
import reconstruction.pattern.LegoPatternReconstructor;
import util.image.ColorMetric;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicMaker {
    private static final boolean DEFAULT_USE_ALPHA = true;
    private static final ColorMetric DEFAULT_COLOR_METRIC = ColorMetric.Euclid2.INSTANCE;
    private static final boolean SVD_RANK_PARAMETER_LOGARITHMIC_SCALE = true;
    private final MosaicMaker<String> mMosaicMaker;
    private MosaicMaker.ProgressCallback progress;
    private int targetWidth;
    private int targetHeight;
    private SVDMaker svdMaker;

    public FileMosaicMaker(List<File> analyzationFiles) {
        Set<MosaicTile<String>> tiles = analyzationFiles.stream().map(FileMosaicJSONBuilder::loadExistingTiles)
                .flatMap(Set::stream).collect(Collectors.toSet());
        System.out.println("Loaded " + tiles.size() + " tiles from " + analyzationFiles.size() + " analyzation files.");
        BitmapSource<String> source = new FileBitmapSource();
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(tiles, DEFAULT_USE_ALPHA, DEFAULT_COLOR_METRIC);
        matcher.setTileReuseLimit(10);
        mMosaicMaker = new MosaicMaker<>(matcher, source, DEFAULT_USE_ALPHA, DEFAULT_COLOR_METRIC);
    }

    public AbstractBitmap makeRect(File sourceFile, int wantedRows, int wantedColumns, MosaicMaker.ProgressCallback progress) {
        AbstractBitmap bitmap = loadSourceFitByRowsColumns(sourceFile, wantedRows, wantedColumns);
        if (bitmap == null) {
            System.err.println("Could not load source: " + sourceFile);
            return null;
        }
        this.progress = progress;
        return mMosaicMaker.makeRect(bitmap, wantedRows, wantedColumns, progress);
    }

    public AbstractBitmap makeMultiRect(File sourceFile, int wantedRows, int wantedColumns, double mergeFactor,
                                        MosaicMaker.ProgressCallback progress) {
        AbstractBitmap bitmap = loadSourceFitByRowsColumns(sourceFile, wantedRows, wantedColumns);
        if (bitmap == null) {
            System.err.println("Could not load source: " + sourceFile);
            return null;
        }
        this.progress = progress;
        return mMosaicMaker.makeMultiRect(bitmap, wantedRows, wantedColumns, mergeFactor, progress);
    }

    public AbstractBitmap makeCircle(File sourceFile, int rows, int columns, MosaicMaker.ProgressCallback progress) {
        AbstractBitmap bitmap = loadSourceFitByRowsColumns(sourceFile, rows, columns);
        if (bitmap == null) {
            System.err.println("Could not load source: " + sourceFile);
            return null;
        }
        this.progress = progress;
        return MosaicMaker.makePattern(bitmap, CirclePatternReconstructor.NAME,
                mMosaicMaker.usesAlpha(), mMosaicMaker.getColorMetric(),
                rows, columns, progress);
    }


    public AbstractBitmap makeSVD(File sourceFile, double factor, MosaicMaker.ProgressCallback callback) {
        AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(sourceFile).createBitmap();
        if (svdMaker == null) {
            svdMaker = new SVDMaker(bitmap, SVDMaker.MODE_INDEXED_BITMAP, callback);
        }
        // use a logarithmic scale as the interesting effects appear in
        // the higher value regions
        int wantedRank;
        if (SVD_RANK_PARAMETER_LOGARITHMIC_SCALE) {
            wantedRank = (int) (Math.log(1. + factor) / Math.log(2) * svdMaker.getMaxRank());
        } else {
            wantedRank = (int) (factor * svdMaker.getMaxRank());
            wantedRank = Math.max(1, wantedRank);
        }
        return svdMaker.getRankApproximation(wantedRank);
    }

    public AbstractBitmap makeLego(File sourceFile, int rows, int columns, MosaicMaker.ProgressCallback progress) {
        AbstractBitmap bitmap = loadSourceFitByRowsColumns(sourceFile, rows, columns);
        if (bitmap == null) {
            System.err.println("Could not load source: " + sourceFile);
            return null;
        }
        this.progress = progress;
        return MosaicMaker.makePattern(bitmap, LegoPatternReconstructor.NAME,
                mMosaicMaker.usesAlpha(), mMosaicMaker.getColorMetric(),
                rows, columns, progress);
    }

    public AbstractBitmap makeFixedLayer(File sourceFile, int clusterCount, MosaicMaker.ProgressCallback progress) {
        AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(sourceFile).createBitmap();
        if (bitmap == null) {
            System.err.println("Could not load source: " + sourceFile);
            return null;
        }
        this.progress = progress;
        return mMosaicMaker.makeFixedLayer(bitmap, clusterCount, progress);
    }

    public AbstractBitmap makeAutoLayer(File sourceFile, double mergeFactor, MosaicMaker.ProgressCallback progress) {
        AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(sourceFile).createBitmap();
        if (bitmap == null) {
            System.err.println("Could not load source: " + sourceFile);
            return null;
        }
        this.progress = progress;
        return mMosaicMaker.makeAutoLayer(bitmap, mergeFactor, progress);
    }

    private AbstractBitmap loadSourceFitByRowsColumns(File sourceFile, int rows, int columns) {
        //make sure that image dimensions are dividable by the given columns/rows values by resizing
        AbstractBitmap base = AbstractBitmapFactory.makeInstance(sourceFile).createBitmap();
        targetWidth = base.getWidth();
        targetHeight = base.getHeight();
        ensureTargetDimensionDivisibleBy(columns, rows, true);
        if (base.getWidth() != targetWidth || base.getHeight() != targetHeight) {
            base.resize(targetWidth, targetHeight);
        }
        return base;
    }

    public void ensureTargetDimensionDivisibleBy(int widthDivisor, int heightDivisor, boolean preferSmaller) {
        // make sure that both dimension are divisible by the given divisor and greater than zero
        int widthDelta = -targetWidth;
        if (preferSmaller) {
            widthDelta = -(targetWidth % widthDivisor);
        }
        if (targetWidth + widthDelta <= 0) {
            widthDelta = widthDivisor + targetWidth % widthDivisor;
        }
        targetWidth += widthDelta;

        int heightDelta = -targetHeight;
        if (preferSmaller) {
            heightDelta = -(targetHeight % heightDivisor);
        }
        if (targetHeight + heightDelta <= 0) {
            heightDelta = heightDivisor + targetHeight % heightDivisor;
        }
        targetHeight += heightDelta;
    }
}
