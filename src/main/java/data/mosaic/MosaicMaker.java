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

package data.mosaic;

import data.image.*;
import matching.TileMatcher;
import reconstruction.MosaicFragment;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import reconstruction.pattern.PatternReconstructor;
import reconstruction.workers.*;
import util.MultistepPercentProgressListener;
import util.PercentProgressListener;
import util.image.Color;
import util.image.ColorSpace;

import java.util.Optional;

/**
 * This class uses a tile matcher as a source pool for MosaicTiles and reconstructs
 * an mosaic for a source bitmap with some Reconstructor type.
 * @param <S>
 */
public class MosaicMaker<S> {
	private final BitmapSource<S> mBitmapSource;
	private TileMatcher<S> mMatcher;
	private ColorSpace space;
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

	public MosaicMaker(TileMatcher<S> tileMatcher, BitmapSource<S> bitmapSource, ColorSpace space) {
		if (tileMatcher == null || bitmapSource == null) {
			throw new IllegalArgumentException("No matcher or source given.");
		}
		mMatcher = tileMatcher;
		mBitmapSource = bitmapSource;
        setColorSpace(space);
	}


    public void setCutResultToSourceAlpha(boolean cutResultToSourceAlpha) {
        this.cutResultToSourceAlpha = cutResultToSourceAlpha;
    }

    private AbstractBitmap finishMosaic(AbstractBitmap source, AbstractBitmap mosaic) {
        if (mosaic == null) {
            return null;
        }
        if (cutResultToSourceAlpha && source != null) {
            System.out.println("Cutting finished mosaic to source alpha.");
            AbstractCanvas canvas = AbstractCanvasFactory.getInstance().makeCanvas(mosaic);
            canvas.drawBitmapUsingPorterDuff(source, 0, 0, PorterDuffMode.DESTINATION_IN);
            return canvas.obtainImage();
        }
        return mosaic;
    }

    public void setColorSpace(ColorSpace space) {
        this.space = space;
        if (space == null) {
            throw new IllegalArgumentException("No color space given.");
        }
        mMatcher.setColorSpace(space);
    }

    public MultiRectReconstructor.MultiRectParameters makeMultiRectParameters(AbstractBitmap source, int wantedRows, int wantedColumns, double mergeFactor, ProgressCallback progress) {
        MultiRectReconstructor.MultiRectParameters params = new MultiRectReconstructor.MultiRectParameters(source);
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        params.similarityFactor = mergeFactor;
        params.space = space;
        return params;
    }

    public RectReconstructor.RectParameters makeRectParameters(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        RectReconstructor.RectParameters params = new RectReconstructor.RectParameters(source);
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        return params;
    }

    public PuzzleReconstructor.PuzzleParameters makePuzzleParameters(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        PuzzleReconstructor.PuzzleParameters params = new PuzzleReconstructor.PuzzleParameters(source);
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        return params;
    }

    public AutoLayerReconstructor.AutoLayerParameters makeAutoLayerParameters(AbstractBitmap source, double mergeFactor, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        AutoLayerReconstructor.AutoLayerParameters params = new AutoLayerReconstructor.AutoLayerParameters(source);
        params.space = space;
        params.factor = mergeFactor;
        params.progress = multiProgress;
        return params;
    }

    public FixedLayerReconstructor.FixedLayerParameters makeFixedLayerParameters(AbstractBitmap source, int clusterCount, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        FixedLayerReconstructor.FixedLayerParameters params = new FixedLayerReconstructor.FixedLayerParameters(source);
        params.space = space;
        params.layersCount = clusterCount;
        params.progress = multiProgress;
        return params;
    }

    public PatternReconstructor.PatternParameters makePatternParameters(AbstractBitmap source, String patternName,
                                                                        int rows, int columns, ProgressCallback progress) {
        PatternReconstructor.PatternParameters params;
        switch (patternName) {
            default:
                // fall through
            case CirclePatternReconstructor.NAME:
                params = new CirclePatternReconstructor.CircleParameters(source);
                break;
            case LegoPatternReconstructor.NAME:
                params = new LegoPatternReconstructor.LegoParameters(source);
                break;
        }
        params.wantedColumns = columns;
        params.wantedRows = rows;
        params.groundingColor = Color.TRANSPARENT;
        return params;
    }

    public AbstractBitmap make(PatternReconstructor.PatternParameters parameters, ProgressCallback callback) throws ReconstructionParameters.IllegalParameterException {
        if (parameters == null) {
            throw new NullPointerException();
        }
        AbstractBitmap source = parameters.getBitmapSource();
        PatternReconstructor reconstructor = parameters.makeReconstructor();
        return finishMosaic(source, make(reconstructor.makeMatcher(parameters.getColorSpace(space)),
                                         reconstructor.makeSource(), reconstructor, callback));
    }

    public AbstractBitmap make(ReconstructionParameters parameters, ProgressCallback callback) throws ReconstructionParameters.IllegalParameterException {
        if (parameters == null) {
            throw new NullPointerException();
        }
        AbstractBitmap source = parameters.getBitmapSource();
        Reconstructor reconstructor = parameters.makeReconstructor();
        return finishMosaic(source, make(mMatcher, mBitmapSource, reconstructor, callback));
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


}
