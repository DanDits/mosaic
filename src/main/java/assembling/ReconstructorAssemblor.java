package assembling;

import data.image.AbstractBitmap;
import data.image.BitmapSource;
import data.mosaic.MosaicTile;
import matching.TileMatcher;
import org.pmw.tinylog.Logger;
import reconstruction.MosaicFragment;
import reconstruction.Reconstructor;
import util.PercentProgressListener;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
public class ReconstructorAssemblor {

    private ReconstructorAssemblor() {}

    //TODO make effects package for stuff like cutBitmapToResourceAlpha, pattern reconstruction and others that
    // TODO are no real mosaics and do not need real reconstruction with a matcher.
    // TODO allow assembler to chain stuff like reconstructors and effects and SVDs
    private static boolean safeIsCancelled(ProgressCallback callback) {
        return callback != null && callback.isCancelled();
    }

    public static <S> Optional<AbstractBitmap> make(TileMatcher<S> matcher, BitmapSource<S> source, Reconstructor
            reconstructor, ProgressCallback progress) {
        Objects.requireNonNull(matcher);
        Objects.requireNonNull(reconstructor);
        Objects.requireNonNull(source);

        while (!reconstructor.hasAll() && !safeIsCancelled(progress)) {
            MosaicFragment nextFrag = reconstructor.nextFragment();
            AbstractBitmap nextImage;
            do {
                Optional<? extends MosaicTile<S>> tileCandidate = matcher.getBestMatch(nextFrag);
                if (!tileCandidate.isPresent()) {
                    // matcher has no more tiles!
                    Logger.error("Matcher out of tiles! Did not find candidate for fragment {}", nextFrag);
                    return Optional.empty();
                }
                MosaicTile<S> tile = tileCandidate.get();
                nextImage = source.getBitmap(tile, nextFrag.getWidth(), nextFrag.getHeight());

                if (nextImage == null) {
                    // no image?! maybe the image (file) got invalid (image deleted, damaged,...)
                    // delete it from matcher and cache and search again
                    matcher.removeTile(tile);
                }
                // will terminate since the matcher will lose a tile each iteration or find a valid one,
                // if no tile found anymore, returns false
            } while (nextImage == null);

            if (!reconstructor.giveNext(nextImage)) {
                // reconstructor did not accept the give image, but it was valid and of correct dimension,
                Logger.error("Reconstructor did not accept given image.");
                return Optional.empty();
            }
            if (progress != null) {
                progress.onProgressUpdate(reconstructor.estimatedProgressPercent());
            }
        }
        if (safeIsCancelled(progress)) {
            return Optional.empty();
        }
        progress.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
        return Optional.of(reconstructor.getReconstructed());
    }
}
