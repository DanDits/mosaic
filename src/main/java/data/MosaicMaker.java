/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package data;

import reconstruction.*;
import util.MultistepPercentProgressListener;
import util.PercentProgressListener;
import util.image.ColorMetric;
import matching.TileMatcher;
import reconstruction.pattern.CirclePatternReconstructor;
import reconstruction.pattern.LegoPatternReconstructor;
import reconstruction.pattern.PatternReconstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class uses a tile matcher as a source pool for MosaicTiles and reconstructs
 * an mosaic for a source bitmap with some Reconstructor type.
 * @param <S>
 */
public class MosaicMaker<S> {
	private final BitmapSource<S> mBitmapSource;
	private TileMatcher<S> mMatcher;
	private boolean mUseAlpha;
	private ColorMetric mColorMetric;
	private boolean cutResultToSourceAlpha;

    public interface ProgressCallback extends PercentProgressListener {
        boolean isCancelled();
    }

    private static class MultiStepPercentProgressCallback extends MultistepPercentProgressListener implements ProgressCallback {

        private final ProgressCallback mCallback;

        public MultiStepPercentProgressCallback(ProgressCallback callback, int steps) {
            super(callback, steps);
            mCallback = callback;
        }


        @Override
        public boolean isCancelled() {
            return mCallback.isCancelled();
        }
    }

	public MosaicMaker(TileMatcher<S> tileMatcher, BitmapSource<S> bitmapSource, boolean useAlpha, ColorMetric metric) {
		if (tileMatcher == null || bitmapSource == null) {
			throw new IllegalArgumentException("No matcher or source given.");
		}
		mMatcher = tileMatcher;
		mBitmapSource = bitmapSource;
        mUseAlpha = useAlpha;
        setColorMetric(metric);
	}


    public void setCutResultToSourceAlpha(boolean cutResultToSourceAlpha) {
        this.cutResultToSourceAlpha = cutResultToSourceAlpha;
    }

    private AbstractBitmap finishMosaic(AbstractBitmap source, AbstractBitmap mosaic) {
        if (mosaic == null) {
            return null;
        }
        if (cutResultToSourceAlpha && source != null) {
            AbstractCanvas canvas = AbstractCanvasFactory.getInstance().makeCanvas(mosaic);
            canvas.drawBitmapUsingPorterDuff(source, 0, 0, PorterDuffMode.DESTINATION_IN);
            return canvas.obtainImage();
        }
        return mosaic;
    }

	public void setUseAlpha(boolean useAlpha) {
		mUseAlpha = useAlpha;
		mMatcher.setUseAlpha(useAlpha);
	}

    public void setColorMetric(ColorMetric metric) {
        mColorMetric = metric;
        if (mColorMetric == null) {
            throw new IllegalArgumentException("No color metric given.");
        }
        mMatcher.setColorMetric(metric);
    }

    public AbstractBitmap makeMultiRect(AbstractBitmap source, int wantedRows, int wantedColumns, double mergeFactor, ProgressCallback progress) {
        List<ImageResolution> allowedResolutions = new ArrayList<>();
        allowedResolutions.add(source.getResolution());
        allowedResolutions.add(new ImageResolution(2, 3));
        allowedResolutions.add(new ImageResolution(3, 2));
        allowedResolutions.add(ImageResolution.SQUARE);
        Reconstructor reconstructor = new NewMultiRectReconstructor(source,
                wantedRows, wantedColumns, allowedResolutions, mColorMetric, mUseAlpha, mergeFactor);
        AbstractBitmap result = make(mMatcher, mBitmapSource, reconstructor, progress);
        return finishMosaic(source, result);
    }

    public AbstractBitmap makeRect(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        Reconstructor reconstructor = new RectReconstructor(source,
                wantedRows, wantedColumns);
        AbstractBitmap result = make(mMatcher, mBitmapSource, reconstructor, progress);
        return finishMosaic(source, result);
    }

    public AbstractBitmap makeAutoLayer(AbstractBitmap source, double mergeFactor, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        Reconstructor reconstructor = new AutoLayerReconstructor(source, mergeFactor, mUseAlpha, mColorMetric, multiProgress);
		multiProgress.nextStep();
        AbstractBitmap result = make(mMatcher, mBitmapSource, reconstructor, multiProgress);
		multiProgress.nextStep();
        return finishMosaic(source, result);
    }

    public AbstractBitmap makeFixedLayer(AbstractBitmap source, int clusterCount, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        Reconstructor reconstructor = new FixedLayerReconstructor(source, clusterCount, mUseAlpha, mColorMetric, multiProgress);
        multiProgress.nextStep();
        AbstractBitmap result = make(mMatcher, mBitmapSource, reconstructor, multiProgress);
        multiProgress.nextStep();
        return finishMosaic(source, result);
    }

    public AbstractBitmap makePattern(AbstractBitmap source, String patternName,
                                     int rows, int columns, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        PatternReconstructor reconstructor;
        switch (patternName) {
            default:
                // fall through
            case CirclePatternReconstructor.NAME:
                reconstructor = new CirclePatternReconstructor(source, rows, columns, AbstractColor.TRANSPARENT);
                break;
            case LegoPatternReconstructor.NAME:
                reconstructor = new LegoPatternReconstructor(source, rows, columns, AbstractColor.TRANSPARENT);
                break;
        }
        multiProgress.nextStep();
        AbstractBitmap result = make(reconstructor.<Void>makeMatcher(usesAlpha(), getColorMetric()), reconstructor
                        .<Void>makeSource(),
                reconstructor,
                multiProgress);
        multiProgress.nextStep();
        return finishMosaic(source, result);
    }

	private static <S>AbstractBitmap make(TileMatcher<S> matcher, BitmapSource<S> source, Reconstructor
            reconstructor, ProgressCallback progress) {
		if (reconstructor == null) {
			throw new IllegalArgumentException("No reconstructor given to make mosaic.");
		}

		while (!reconstructor.hasAll() && (progress == null || !progress.isCancelled())) {
			MosaicFragment nextFrag = reconstructor.nextFragment();
			AbstractBitmap nextImage;
			do {
				Optional<? extends MosaicTile<S>> tileCandidate = matcher.getBestMatch(nextFrag);
				if (!tileCandidate.isPresent()) {
                    // matcher has no more tiles!
                    System.err.println("Matcher out of tiles!");
                    return null;
                }
                MosaicTile<S> tile = tileCandidate.get();
				nextImage = source.getBitmap(tile, nextFrag.getWidth(), nextFrag.getHeight());

				if (nextImage == null) {
					// no image?! maybe the image (file) got invalid (image deleted, damaged,...)
					// delete it from matcher
					// and cache and search again
					matcher.removeTile(tile);
				}
				// will terminate since the matcher will lose a tile each iteration or find a valid one, 
				// if no tile found anymore, returns false
			} while (nextImage == null);

			if (!reconstructor.giveNext(nextImage)) {
				// reconstructor did not accept the give image, but it was valid and of correct dimension,
                System.err.println("Reconstructor did not accept given image.");
                return null;
			}
            if (progress != null) {
                progress.onProgressUpdate(reconstructor.estimatedProgressPercent());
            }
		}
		if (progress != null && !progress.isCancelled()) {
			progress.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
		}
        if (progress != null && progress.isCancelled()) {
            return null;
        }
		return reconstructor.getReconstructed();
	}

    public boolean usesAlpha() {
        return mUseAlpha;
    }

    public ColorMetric getColorMetric() {
        return mColorMetric;
    }
}
