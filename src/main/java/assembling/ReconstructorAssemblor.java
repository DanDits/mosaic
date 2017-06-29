package assembling;

import data.image.AbstractBitmap;
import data.image.BitmapSource;
import data.storage.MosaicTile;
import effects.BitmapEffect;
import effects.EffectGroup;
import matching.TileMatcher;
import org.pmw.tinylog.Logger;
import reconstruction.MosaicFragment;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import ui.FileMosaicJSONBuilder;
import util.PercentProgressListener;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The typo in assemblor is intentional to distinguish it from classic assemblers that built something in machine code!
 * #FreedomOfArt
 *
 * Created by dd on 28.06.17.
 */
public class ReconstructorAssemblor {

    private ReconstructorAssemblor() {}

    private static boolean safeIsCancelled(ProgressCallback callback) {
        return callback != null && callback.isCancelled();
    }

    public static Collection<MosaicTile<String>> loadTilesFromFiles(List<File> analyzationFiles) {
        Set<MosaicTile<String>> tiles = analyzationFiles.stream().map(FileMosaicJSONBuilder::loadExistingTiles)
                                                          .flatMap(Set::stream).collect(Collectors.toSet());

        Logger.info("Loaded {} tiles from {} analyzation files.", tiles.size(), analyzationFiles.size());
        return tiles;
    }

    public static <S> BitmapEffect makeEffect(TileMatcher<S> matcher, BitmapSource<S> source,
                                              ReconstructionParameters reconstructorParams, ProgressCallback callback) {
        return EffectGroup.createUsingPreAndPostEffects(new MosaicEffect<>(matcher, source, reconstructorParams, callback));
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
                    Logger.error("ReconstructorAssemblor. Matcher out of tiles! Did not find candidate for fragment {}. Aborting.", nextFrag);
                    return Optional.empty();
                }
                MosaicTile<S> tile = tileCandidate.get();
                nextImage = source.getBitmap(tile, nextFrag.getWidth(), nextFrag.getHeight());

                if (nextImage == null) {
                    // no image?! maybe the image (file) got invalid (image deleted, damaged,...)
                    // delete it from matcher and cache and search again
                    Logger.error("ReconstructorAssemblor. Could not load image for tile {}. Retrying.", tile);
                    matcher.removeTile(tile);
                }
                // will terminate since the matcher will lose a tile each iteration or find a valid one,
                // if no tile found anymore, returns false
            } while (nextImage == null);

            if (!reconstructor.giveNext(nextImage)) {
                // reconstructor did not accept the give image, but it was valid and of correct dimension,
                Logger.error("ReconstructorAssemblor. Did not accept given image. Aborting.");
                return Optional.empty();
            }
            if (progress != null) {
                progress.onProgressUpdate(reconstructor.estimatedProgressPercent());
            }
        }
        if (safeIsCancelled(progress)) {
            return Optional.empty();
        }
        if (progress != null) {
            progress.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
        }
        return Optional.of(reconstructor.getReconstructed());
    }

    private static class MosaicEffect<S> implements BitmapEffect {
        private TileMatcher<S> matcher;
        private BitmapSource<S> source;
        private ReconstructionParameters reconstructorParams;
        private ProgressCallback callback;

        MosaicEffect(TileMatcher<S> matcher, BitmapSource<S> source, ReconstructionParameters reconstructor, ProgressCallback callback) {
            this.matcher = matcher;
            this.source = source;
            this.reconstructorParams = reconstructor;
            this.callback = callback;
        }

        @Override
        public List<BitmapEffect> getPreEffects() {
            return reconstructorParams.getPreReconstructionEffects();
        }

        @Override
        public List<BitmapEffect> getPostEffects() {
            return reconstructorParams.getPostReconstructionEffects();
        }

        @Override
        public Optional<AbstractBitmap> apply(AbstractBitmap on) {
            Reconstructor reconstructor;
            try {
                 reconstructor = reconstructorParams.setSourceBitmap(on).makeReconstructor();
            } catch (ReconstructionParameters.IllegalParameterException e) {
                Logger.error("Illegal reconstructor parameters: {}", e);
                return Optional.empty();
            }
            return ReconstructorAssemblor.make(matcher, source, reconstructor, callback);
        }
    }
}
