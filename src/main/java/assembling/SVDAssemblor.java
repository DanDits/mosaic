package assembling;

import data.image.AbstractBitmap;
import effects.BitmapEffect;
import util.ProgressCallback;

import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
public class SVDAssemblor {
    private SVDAssemblor () {}

    public static BitmapEffect makeEffect(int mode, double factor, ProgressCallback callback) {
        return new SVDEffect(mode, factor, callback);
    }

    private static class SVDEffect implements BitmapEffect {
        private static final boolean DEFAULT_USE_LOGARITHMIC_SCALE = true;
        private final int mode;
        private ProgressCallback callback;
        private boolean useLogarithmicScale;
        private double factor;

        private SVDEffect(int mode, double factor, ProgressCallback callback) {
            this.mode = mode;
            useLogarithmicScale = DEFAULT_USE_LOGARITHMIC_SCALE;
            this.callback = callback;
            this.factor = factor;
        }

        @Override
        public Optional<AbstractBitmap> apply(AbstractBitmap source) {
            SVDMaker maker = new SVDMaker(source, mode, callback);
            int wantedRank = getWantedRank(maker);
            return Optional.ofNullable(maker.getRankApproximation(wantedRank));
        }

        private int getWantedRank(SVDMaker maker) {
            int wantedRank;
            if (useLogarithmicScale) {
                // use a logarithmic scale as the interesting effects appear in
                // the higher value regions
                wantedRank = (int) (Math.log(1. + factor) / Math.log(2) * maker.getMaxRank());
            } else {
                wantedRank = (int) (factor * maker.getMaxRank());
                wantedRank = Math.max(1, wantedRank);
            }
            return wantedRank;
        }
    }
}
