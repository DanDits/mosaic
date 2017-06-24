package matching.workers;

import data.mosaic.MosaicTile;
import matching.TileMatcher;
import org.pmw.tinylog.Logger;
import reconstruction.MosaicFragment;
import util.image.ColorSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by daniel on 09.06.17.
 */
public class ResolutionMatcher<S> extends TileMatcher<S> {
    private final List<MosaicTile<S>> tiles;
    private double accuracy;

    public ResolutionMatcher(Collection<? extends MosaicTile<S>> data, double accuracy, ColorSpace space) {
        super(space);
        this.tiles = new ArrayList<>(data);
        setAccuracy(accuracy);
    }


    @Override
    protected void onColorSpaceChanged() {

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
        }
        if (useTiles.size() == 0 && wantedTile.getWidth() > 0 && wantedTile.getHeight() > 0) {
            final double wantedFraction = wantedTile.getWidth() / (double) wantedTile.getHeight();
            Logger.trace("ResolutionMatcher dropped every tile when using accuracy {} for resolution {}x{}, now searching for best fit.",
                        accuracy, wantedTile.getWidth(), wantedTile.getHeight());
            // panic, we want to get something at least
            return tiles.stream()
                    .min(Comparator.comparingDouble(tile -> getResolutionDifference(tile, wantedFraction)));
        }
        Logger.trace("ResolutionMatcher with accuracy {} got {}/{} tile(s) with fitting resolution {}x{}",
                     accuracy, useTiles.size(), tiles.size(), wantedTile.getWidth(), wantedTile.getHeight());
        return useTiles.stream().min(Comparator.comparingDouble(
                tile -> space.getDistance(tile.getAverageARGB(), wantedTile.getAverageRGB())));
    }

    private static double accuracyToAllowedDifference(double accuracy) {
        // constraints: infinity for accuracy=0, zero for accuracy=1, monotonous and continuous in between
        //return (1. / accuracy - 1) / 10. + 1E-7;
        return -0.1 * Math.log(accuracy);
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
