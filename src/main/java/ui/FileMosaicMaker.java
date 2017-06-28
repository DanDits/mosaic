package ui;

import assembling.MosaicMaker;
import assembling.ProgressCallback;
import assembling.ReconstructorAssemblor;
import assembling.SVDMaker;
import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.image.BitmapSource;
import data.storage.MosaicTile;
import matching.TileMatcher;
import matching.workers.FastMatcher;
import matching.workers.RandomMatcher;
import matching.workers.ResolutionMatcher;
import matching.workers.SimpleLinearTileMatcher;
import util.image.ColorSpace;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicMaker {
    private static final ColorSpace DEFAULT_COLOR_SPACE = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
    private static final boolean SVD_RANK_PARAMETER_LOGARITHMIC_SCALE = true;
    private final MosaicMaker<String> mMosaicMaker;
    private ProgressCallback progress;
    private int targetWidth;
    private int targetHeight;
    private SVDMaker svdMaker;

    public FileMosaicMaker(List<File> analyzationFiles) {

        BitmapSource<String> source = new FileBitmapSource();
        Collection<MosaicTile<String>> tiles = ReconstructorAssemblor.loadTilesFromFiles(analyzationFiles);
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(tiles, DEFAULT_COLOR_SPACE);
        matcher.setTileReuseLimit(TileMatcher.REUSE_NONE);

        RandomMatcher<String> matcherRandom = new RandomMatcher<>(tiles);
        matcherRandom.setTileReuseLimit(TileMatcher.REUSE_NONE);
        matcherRandom.setRandom(new Random(46));

        ResolutionMatcher<String> resolutionMatcher = new ResolutionMatcher<>(tiles, 0.9, DEFAULT_COLOR_SPACE);


        FastMatcher<String> fastMatcher = new FastMatcher<>(tiles, DEFAULT_COLOR_SPACE);
        fastMatcher.setTileReuseLimit(TileMatcher.REUSE_NONE);

        mMosaicMaker = new MosaicMaker<>(matcher, source, DEFAULT_COLOR_SPACE);
        //mMosaicMaker.setCutResultToSourceAlpha(true);
    }

    public MosaicMaker<String> getMaker() {
        return mMosaicMaker;
    }

    public AbstractBitmap makeSVD(File sourceFile, double factor, ProgressCallback callback) {
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

    public AbstractBitmap loadSourceFitByRowsColumns(File sourceFile, int rows, int columns) {
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
