package matching.workers;

import matching.TileMatcher;
import reconstruction.MosaicFragment;
import util.image.ColorMetric;
import data.mosaic.MosaicTile;

import java.util.Optional;

/**
 * Created by daniel on 05.12.15.
 */
public class TrivialMatcher<S> extends TileMatcher<S> {
    private NullTile mTile;
    private final Optional<NullTile> mOptionalTile;

    public TrivialMatcher() {
        super(true, ColorMetric.Absolute.INSTANCE);
        mTile = new NullTile();
        mOptionalTile = Optional.of(mTile);
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
    public boolean removeTile(MosaicTile<S> toRemove) {
        return false;
    }

    @Override
    public int getUsedTilesCount() {
        return 1;
    }

}
