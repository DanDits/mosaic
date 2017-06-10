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

    public ResolutionMatcher(Collection<? extends MosaicTile<S>> data, double accuracy, boolean useAlpha, ColorMetric metric) {
        super(useAlpha, metric);
        this.tiles = new ArrayList<>(data);
        setAccuracy(accuracy);
    }


    @Override
    protected Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedTile) {
        List<MosaicTile<S>> useTiles = tiles;
        if (wantedTile.getWidth() > 0 && wantedTile.getHeight() > 0) {
            final double wantedFraction = wantedTile.getWidth() / (double) wantedTile.getHeight();
            final double allowedDifference = accuracyToAllowedDifference(accuracy);
            useTiles = tiles.stream()
                    .filter(tile -> getResolutionDifference(tile, wantedFraction) <= allowedDifference)
                    .collect(Collectors.toList());
            System.out.println("Allowed difference=" + allowedDifference);
        }
        if (useTiles.size() == 0 && wantedTile.getWidth() > 0 && wantedTile.getHeight() > 0) {
            final double wantedFraction = wantedTile.getWidth() / (double) wantedTile.getHeight();
            System.out.println("Dropped every tile when using accuracy " + accuracy + " for resolution " + wantedTile.getWidth() + "x" + wantedTile.getHeight() + " now searching for best fit.");
            // panic, we want to get something at least
            Optional<MosaicTile<S>> tileCandidate = tiles.stream()
                    .min(Comparator.comparingDouble(tile -> getResolutionDifference(tile, wantedFraction)));
            return tileCandidate;
        }
        System.out.println("Got " + useTiles.size() + " tiles with fitting resolution: " + wantedTile.getWidth() + "x" + wantedTile.getHeight());
        return useTiles.stream().min(Comparator.comparingDouble(
                tile -> mColorMetric.getDistance(tile.getAverageARGB(), wantedTile.getAverageRGB(), useAlpha)));
    }

    private static double accuracyToAllowedDifference(double accuracy) {
        // constraints: infinity for accuracy=0, zero for accuracy=1, monotonous and continuous in between
        //return (1. / accuracy - 1) / 10. + 1E-7;
        return -Math.log(accuracy);
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
        return tiles.remove(toRemove);
    }

    @Override
    public int getUsedTilesCount() {
        return this.tiles.size();
    }
}
