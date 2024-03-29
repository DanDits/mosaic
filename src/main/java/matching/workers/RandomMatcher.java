package matching.workers;

import data.storage.MosaicTile;
import matching.TileMatcher;
import reconstruction.MosaicFragment;

import java.util.*;

/**
 * Created by dd on 07.06.17.
 */
public class RandomMatcher<S> extends TileMatcher<S> {

    private final List<MosaicTile<S>> tiles;
    private Random random;

    public RandomMatcher(Collection<MosaicTile<S>> data) {
        super(null);
        tiles = new ArrayList<>(data);
        setRandom(new Random());
    }

    public void setRandom(Random random) {
        this.random = random;
        if (random == null) {
            throw new NullPointerException();
        }
    }

    @Override
    protected void onColorSpaceChanged() {
        // ignore
    }

    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedFragment) {
        if (tiles.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tiles.get(random.nextInt(tiles.size())));
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
    public boolean doRemoveTile(MosaicTile<S> toRemove) {
        return tiles.remove(toRemove);
    }

    @Override
    public int getUsedTilesCount() {
        return tiles.size();
    }
}
