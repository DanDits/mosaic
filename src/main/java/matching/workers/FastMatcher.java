package matching.workers;

import data.mosaic.MosaicTile;
import matching.TileMatcher;
import reconstruction.MosaicFragment;
import util.image.ColorMetric;

import java.util.Optional;

/**
 * Uses a K-D tree (see https://en.wikipedia.org/wiki/K-d_tree) to search for a nearest neighbor as the best match.
 * This requires a longer initialization time (O(nlog(n))), but gives a faster lookup time (O(log(n)). The dimensions
 * are the (a)rgb color components. The overhead is only worth if there are much more than 2^dimension=16 elements used
 * by the matcher.
 * Created by dd on 22.06.17.
 */
public class FastMatcher<S> extends TileMatcher<S> {

    protected FastMatcher(boolean useAlpha, ColorMetric metric) {
        super(useAlpha, metric);
    }

    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedTile) {
        return null;
    }

    @Override
    public void setUseAlpha(boolean useAlpha) {
        boolean oldAlpha = usesAlpha();
        super.setUseAlpha(useAlpha);
        // TODO reinit tree if dimension changes
    }

    @Override
    public double getAccuracy() {
        return 0;
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
        return 0;
    }
}
