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

package assembling;

import data.image.*;
import matching.TileMatcher;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import reconstruction.pattern.PatternReconstructor;
import reconstruction.workers.*;
import util.image.Color;
import util.image.ColorSpace;

import java.util.Optional;


public class MosaicMaker<S> {
    // TODO ultimatively this class is only supposed to offer some default and example assemblors
    // TODO and serves as a factory
	private final BitmapSource<S> mBitmapSource;
	private TileMatcher<S> mMatcher;
	private ColorSpace space;
	private boolean cutResultToSourceAlpha;


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
        MultiRectReconstructor.MultiRectParameters params = new MultiRectReconstructor.MultiRectParameters();
        params.source = source;
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        params.similarityFactor = mergeFactor;
        params.space = space;
        return params;
    }

    public RectReconstructor.RectParameters makeRectParameters(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        RectReconstructor.RectParameters params = new RectReconstructor.RectParameters();
        params.source = source;
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        return params;
    }

    public PuzzleReconstructor.PuzzleParameters makePuzzleParameters(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        PuzzleReconstructor.PuzzleParameters params = new PuzzleReconstructor.PuzzleParameters();
        params.source = source;
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        return params;
    }

    public AutoLayerReconstructor.AutoLayerParameters makeAutoLayerParameters(AbstractBitmap source, double mergeFactor, ProgressCallback progress) {
        MultiStepProgressCallback multiProgress = new MultiStepProgressCallback(progress, 2);
        AutoLayerReconstructor.AutoLayerParameters params = new AutoLayerReconstructor.AutoLayerParameters();
        params.source = source;
        params.space = space;
        params.factor = mergeFactor;
        params.progress = multiProgress;
        return params;
    }

    public FixedLayerReconstructor.FixedLayerParameters makeFixedLayerParameters(AbstractBitmap source, int clusterCount, ProgressCallback progress) {
        MultiStepProgressCallback multiProgress = new MultiStepProgressCallback(progress, 2);
        FixedLayerReconstructor.FixedLayerParameters params = new FixedLayerReconstructor.FixedLayerParameters();
        params.source = source;
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
                params = new CirclePatternReconstructor.CircleParameters();
                break;
            case LegoPatternReconstructor.NAME:
                params = new LegoPatternReconstructor.LegoParameters();
                break;
        }
        params.source = source;
        params.wantedColumns = columns;
        params.wantedRows = rows;
        params.groundingColor = Color.TRANSPARENT;
        return params;
    }

    public AbstractBitmap make(PatternReconstructor.PatternParameters parameters, ProgressCallback callback) throws ReconstructionParameters.IllegalParameterException {
        if (parameters == null) {
            throw new NullPointerException();
        }
        AbstractBitmap source = parameters.source;
        PatternReconstructor reconstructor = parameters.makeReconstructor();
        Optional<AbstractBitmap> bmp = ReconstructorAssemblor.make(reconstructor.makeMatcher(parameters.getColorSpace(space)),
                                                                   reconstructor.makeSource(), reconstructor, callback);
        return bmp.map(abstractBitmap -> finishMosaic(source, abstractBitmap)).orElse(null);
    }

    public AbstractBitmap make(ReconstructionParameters parameters, ProgressCallback callback) throws ReconstructionParameters.IllegalParameterException {
        if (parameters == null) {
            throw new NullPointerException();
        }
        AbstractBitmap source = parameters.source;
        Reconstructor reconstructor = parameters.makeReconstructor();
        Optional<AbstractBitmap> bmp = ReconstructorAssemblor.make(mMatcher, mBitmapSource, reconstructor, callback);
        return bmp.map(abstractBitmap -> finishMosaic(source, abstractBitmap)).orElse(null);
    }




}
