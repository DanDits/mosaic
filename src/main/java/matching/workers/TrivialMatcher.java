package matching.workers;

import data.storage.MosaicTile;
import matching.TileMatcher;
import reconstruction.MosaicFragment;

import java.util.Optional;

/**
 * Created by daniel on 05.12.15.
 */
public class TrivialMatcher<S> extends TileMatcher<S> {
    private NullTile mTile;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")  // to prevent having to wrap over and over again
    private final Optional<NullTile> mOptionalTile;

    public TrivialMatcher() {
        super(null);
        mTile = new NullTile();
        mOptionalTile = Optional.of(mTile);
    }

    @Override
    protected boolean cacheEnabled() {
        return false; // not needed
    }

    @Override
    protected void onColorSpaceChanged() {
        //ignore
    }

    private class NullTile implements MosaicTile<S> {
        private int mAverageColor;
        private int width;
        private int height;
        @Override
        public S getSource() {
            return null;
        }

        @Override
        public int getAverageARGB() {
            return mAverageColor;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedTile) {
        mTile.mAverageColor = wantedTile.getAverageRGB();
        mTile.width = wantedTile.getWidth();
        mTile.height = wantedTile.getHeight();
        return mOptionalTile;
    }

    @Override
    public double getAccuracy() {
        return 1.;
    }

    @Override
    public boolean setAccuracy(double accuracy) {
        return false;
    }

    @Override
    public boolean doRemoveTile(MosaicTile<S> toRemove) {
        return false;
    }

    @Override
    public int getUsedTilesCount() {
        return 1;
    }

}
