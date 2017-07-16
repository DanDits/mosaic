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

import data.export.AbstractBitmapExporter;
import data.image.AbstractBitmap;
import data.image.BitmapSource;
import data.storage.MosaicTile;
import matching.TileMatcher;
import matching.workers.FastMatcher;
import matching.workers.SimpleLinearTileMatcher;
import reconstruction.ReconstructionParameters;
import reconstruction.workers.*;
import util.image.ColorSpace;

import java.util.Collection;
import java.util.Objects;


public class MosaicMaker<S> {
	private final BitmapSource<S> bitmapSource;
    private final AbstractBitmapExporter exporter;
    private TileMatcher<S> matcher;
	private ColorSpace space;
	private Collection<MosaicTile<S>> tiles;


    public MosaicMaker(BitmapSource<S> bitmapSource, ColorSpace space, Collection<MosaicTile<S>> tiles,
                       AbstractBitmapExporter exporter) {
		Objects.requireNonNull(bitmapSource);
		Objects.requireNonNull(exporter);
		this.tiles = tiles;
        setMatcherAutomatically();
		setColorSpace(space);
		this.bitmapSource = bitmapSource;
		this.exporter = exporter;
	}

    private void setMatcherAutomatically() {
        if (tiles.size() > 5000) {
            matcher = new FastMatcher<>(tiles, space);
        } else {
            matcher = new SimpleLinearTileMatcher<>(tiles, space);
        }
    }

    public void setMatcher(TileMatcher<S> matcher) {
        Objects.requireNonNull(matcher);
        this.matcher = matcher;
        this.matcher.setColorSpace(space);
    }

    public void setColorSpace(ColorSpace space) {
        Objects.requireNonNull(space);
        this.space = space;
        matcher.setColorSpace(space);
    }

    private BitmapProject makeReconstructorProject(ReconstructionParameters parameters, ProgressCallback callback) {
        return new BitmapProject(ReconstructorAssemblor.makeEffect(matcher, bitmapSource, parameters, callback), exporter);
    }

    public BitmapProject makeMultiRectProject(AbstractBitmap source, int wantedRows, int wantedColumns, double mergeFactor, ProgressCallback progress) {
        MultiRectReconstructor.MultiRectParameters params = new MultiRectReconstructor.MultiRectParameters();
        params.source = source;
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        params.similarityFactor = mergeFactor;
        params.space = space;
        return makeReconstructorProject(params, progress);
    }

    public BitmapProject makeRectProject(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        RectReconstructor.RectParameters params = new RectReconstructor.RectParameters();
        params.source = source;
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        return makeReconstructorProject(params, progress);
    }

    public BitmapProject makePuzzleProject(AbstractBitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        PuzzleReconstructor.PuzzleParameters params = new PuzzleReconstructor.PuzzleParameters();
        params.source = source;
        params.wantedColumns = wantedColumns;
        params.wantedRows = wantedRows;
        return makeReconstructorProject(params, progress);
    }

    public BitmapProject makeAutoLayerProject(AbstractBitmap source, double mergeFactor, ProgressCallback progress) {
        MultiStepProgressCallback multiProgress = new MultiStepProgressCallback(progress, 2);
        AutoLayerReconstructor.AutoLayerParameters params = new AutoLayerReconstructor.AutoLayerParameters();
        params.source = source;
        params.space = space;
        params.factor = mergeFactor;
        params.progress = multiProgress;
        return makeReconstructorProject(params, progress);
    }

    public BitmapProject makeFixedLayerProject(AbstractBitmap source, int clusterCount, ProgressCallback progress) {
        MultiStepProgressCallback multiProgress = new MultiStepProgressCallback(progress, 2);
        FixedLayerReconstructor.FixedLayerParameters params = new FixedLayerReconstructor.FixedLayerParameters();
        params.source = source;
        params.space = space;
        params.layersCount = clusterCount;
        params.progress = multiProgress;
        return makeReconstructorProject(params, progress);
    }

    public BitmapProject makeSVD(AbstractBitmap source, double mergeFactor, ProgressCallback progress) {
        return new BitmapProject(SVDAssemblor.makeEffect(SVDMaker.MODE_RGB_SPLIT, mergeFactor, progress), exporter);
    }
}
