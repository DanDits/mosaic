package matching;

import util.image.ColorMetric;
import data.MosaicTile;

import java.util.Optional;

/**
 * Created by daniel on 05.12.15.
 */
public class TrivialMatcher<S> extends TileMatcher<S> {
    private final Optional<NullTile> mTile;
    public TrivialMatcher() {
        super(true, ColorMetric.Absolute.INSTANCE);
        mTile = Optional.of(new NullTile());
    }

    private class NullTile implements MosaicTile<S> {
        private int mAverageColor;
        @Override
        public S getSource() {
            return null;
        }

        @Override
        public int getAverageARGB() {
            return mAverageColor;
        }
    }

    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(int withRGB) {
        mTile.get().mAverageColor = withRGB;
        return mTile;
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
