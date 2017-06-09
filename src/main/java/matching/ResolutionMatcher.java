package matching;

import data.MosaicTile;
import reconstruction.MosaicFragment;
import util.image.ColorMetric;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by daniel on 09.06.17.
 */
public class ResolutionMatcher<S> extends TileMatcher<S> {
    private final List<MosaicTile<S>> tiles;
    private double accuracy;

    ResolutionMatcher(Collection<? extends MosaicTile<S>> data, double accuracy, boolean useAlpha, ColorMetric metric) {
        super(useAlpha, metric);
        this.tiles = new ArrayList<>(data);
        setAccuracy(accuracy);
    }


    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedTile) {
        List<MosaicTile<S>> useTiles = tiles;
        if (wantedTile.getWidth() > 0 && wantedTile.getHeight() > 0) {
            double wantedFraction = wantedTile.getWidth() / (double) wantedTile.getHeight();
            useTiles = tiles.stream().sorted(Comparator.comparingDouble(tile -> getResolutionDifference(tile, wantedFraction)))
                    // TODO later
                    .collect(Collectors.toList());
        }
        return useTiles.stream().min(Comparator.comparingDouble(
                tile -> mColorMetric.getDistance(tile.getAverageARGB(), wantedTile.getAverageRGB(), useAlpha)));
    }

    private double getResolutionDifference(MosaicTile<S> tile, double wantedFraction) {
        if (tile.getWidth() <= 0 || tile.getHeight() <= 0) {
            return Double.MAX_VALUE;
        }
        return Math.abs(tile.getWidth() / (double) tile.getHeight() - wantedFraction);
    }

    @Override
    public double getAccuracy() {
        return accuracy;
    }

    @Override
    public boolean setAccuracy(double accuracy) {
        this.accuracy = Math.min(1., Math.max(0., accuracy));
        return true;
    }

    @Override
    public boolean removeTile(MosaicTile<S> toRemove) {
        return false; //TODO implement
    }

    @Override
    public int getUsedTilesCount() {
        return this.tiles.size();
    }
}
