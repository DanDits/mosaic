package matching.workers;

import data.mosaic.MosaicTile;
import matching.TileMatcher;
import reconstruction.MosaicFragment;
import util.image.ColorMetric;
import util.image.KDColorTree;

import java.util.*;

/**
 * Uses a K-D tree (see https://en.wikipedia.org/wiki/K-d_tree) to search for a nearest neighbor as the best match.
 * This requires a longer initialization time (O(nlog(n))), but gives a faster lookup time (O(log(n)). The dimensions
 * are the (a)rgb color components. The overhead is only worth if there are much more than 2^dimension=16 elements used
 * by the matcher and best match is queried often.
 * Changing if the matcher uses alpha will do a costly reinitialization of the tree. Removing of tiles can result in an
 * unbalanced tree and therefore worsen the performance if many tiles are removed. Thus you should prefer to use this matcher for unlimited (or high)
 * reuse.
 * Created by dd on 22.06.17.
 */
public class FastMatcher<S> extends TileMatcher<S> {

    private KDColorTree<MosaicTile<S>> tree;
    private final List<MosaicTile<S>> tiles;

    protected FastMatcher(Collection<? extends MosaicTile<S>> tiles, boolean useAlpha) {
        super(useAlpha, ColorMetric.Euclid2.INSTANCE);
        this.tiles = new ArrayList<>(tiles);
        initTree();
    }

    private void initTree() {
        tree = KDColorTree.make(new Random(), tiles, MosaicTile::getAverageARGB, useAlpha);
    }

    @Override
    public void setColorMetric(ColorMetric metric) {
        // ignore
    }

    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedTile) {
        return tree.getNearestNeighbor(wantedTile.getAverageRGB());
    }

    @Override
    public void setUseAlpha(boolean useAlpha) {
        boolean oldAlpha = usesAlpha();
        super.setUseAlpha(useAlpha);
        if (oldAlpha != useAlpha) {
            initTree();
        }
    }

    @Override
    public double getAccuracy() {
        return 1;
    }

    @Override
    public boolean setAccuracy(double accuracy) {
        return false;
    }

    @Override
    public boolean removeTile(MosaicTile<S> toRemove) {
        return tree.removeNode(toRemove.getAverageARGB(), toRemove);
    }

    @Override
    public int getUsedTilesCount() {
        return tree.size();
    }
}
