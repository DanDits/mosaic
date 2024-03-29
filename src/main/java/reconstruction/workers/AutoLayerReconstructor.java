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

package reconstruction.workers;


import data.image.AbstractBitmap;
import reconstruction.MosaicFragment;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import util.MultistepPercentProgressListener;
import util.PercentProgressListener;
import util.image.ColorAnalysisUtil;
import util.image.ColorSpace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by daniel on 01.07.15.
 */
public class AutoLayerReconstructor extends Reconstructor {
    private AbstractBitmap mResult;
    private MosaicFragment mNext;
    private int mLayersApplied;
    private Iterator<Integer> mColorIterator;
    private List<List<Integer>> mUsedColorsStartPosition;
    private List<Integer> mUsedColors;
    private int[] mPositionDeltas;
    private ColorSpace space;

    public static class AutoLayerParameters extends ReconstructionParameters {
        private static final ColorSpace DEFAULT_SPACE = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        public MultistepPercentProgressListener progress;
        public double factor;
        public ColorSpace space;


        @Override
        public Reconstructor makeReconstructor() throws IllegalParameterException {
            Reconstructor reconstructor = new AutoLayerReconstructor(this);
            if (progress != null) {
                progress.nextStep();
            }
            return reconstructor;
        }

        @Override
        protected void resetToDefaults() {
            space = DEFAULT_SPACE;
            factor = 0.7;
            progress = null;
        }

        @Override
        protected void validateParameters() throws IllegalParameterException {
            super.validateParameters();
            if (space == null) {
                space = DEFAULT_SPACE;
            }
            factor = Math.min(1., Math.max(0., factor));
        }
    }

    public AutoLayerReconstructor(AutoLayerParameters parameters) throws ReconstructionParameters.IllegalParameterException {
        parameters.validateParameters();
        space = parameters.space;
        init(parameters.source, parameters.factor, parameters.progress);
    }

    private void init(AbstractBitmap source, double factor, PercentProgressListener progress) {

        final int width = source.getWidth();
        final int height = source.getHeight();
        mResult = obtainBaseBitmap(width, height);
        final int[] colors = new int[width * height];
        final int[] deltas = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                colors[x + y * width] = source.getPixel(x, y);
            }
        }
        final double maxSim = space.getMaxDistance();
        final double sim = ColorAnalysisUtil.factorToSimilarityBound(factor);
        final int simBound = (int) (sim * maxSim);
        final int alreadyReachedMarker = Integer.MIN_VALUE;
        List<Integer> usedColors = new ArrayList<>();
        List<Integer> usedColorsStartPosition = new ArrayList<>();

        for (int analyzedToIndex = 0; analyzedToIndex < colors.length; analyzedToIndex++) {
            // first try to evolve the current color further, under the assumption that similar colors are close to each other this is almost constant in operations
            int currColor = colors[analyzedToIndex];
            if (deltas[analyzedToIndex] == 0) {
                // color not yet reached, use it as start color
                usedColors.add(currColor);
                usedColorsStartPosition.add(analyzedToIndex);
            }
            deltas[analyzedToIndex] = 0; // marker no  longer required, reset delta

            for (int currIndex = analyzedToIndex + 1; currIndex < colors.length; currIndex++) {
                if (space.getDistance(colors[currIndex], currColor) <= simBound) {
                    colors[currIndex] = currColor;
                    deltas[analyzedToIndex] = currIndex - analyzedToIndex;
                    deltas[currIndex] = alreadyReachedMarker; // so it is not added as a new color when main loop reaches this index
                    break;
                }
            }
            progress.onProgressUpdate((int) (100 * analyzedToIndex / (double) colors.length));
        }


        // now create actual color circles around the filtered used colors, only required if too many colors matched above that are similar but didnt know it
        mUsedColorsStartPosition = new LinkedList<>();
        mUsedColors = new LinkedList<>();
        for (int i = 0; i < usedColors.size(); i++) {
            int currColorI = usedColors.get(i);
            mUsedColors.add(currColorI);
            List<Integer> startPositions = new LinkedList<>();
            mUsedColorsStartPosition.add(startPositions);
            startPositions.add(usedColorsStartPosition.get(i));
            for (int j = i + 1; j < usedColors.size(); j++) {
                Integer potentialColor = usedColors.get(j);
                if (space.getDistance(currColorI, potentialColor) <= simBound) { // if the pixel != 0 check is not done you need to check here if delta is zero else multiple paths might leed together and result in way too many pixels being drawn
                    startPositions.add(usedColorsStartPosition.get(j));
                    usedColors.remove(j);
                    usedColorsStartPosition.remove(j);
                    j--;
                }
            }
        }


        mPositionDeltas = deltas;
    }

    @Override
    public boolean giveNext(AbstractBitmap nextFragmentImage) {
        if (nextFragmentImage == null || mNext == null
                || nextFragmentImage.getWidth() != mResult.getWidth() || nextFragmentImage.getHeight() != mResult.getHeight()) {
            return false;
        }
        int width = mResult.getWidth();
        List<Integer> startPositions = mUsedColorsStartPosition.get(mLayersApplied);

        for (int position : startPositions) {
            int delta;
            do {
                int x = position % width;
                int y = position / width;
                if (mResult.getPixel(x, y) != 0) {
                    break;
                }
                mResult.setPixel(x, y, nextFragmentImage.getPixel(x, y));
                delta = mPositionDeltas[position];
                position += delta;
            } while (delta > 0);
        }
        mNext = null;
        mLayersApplied++;
        return true;
    }

    @Override
    public MosaicFragment nextFragment() {
        if (mColorIterator == null) {
            mColorIterator = mUsedColors.iterator();
        }
        if (!mColorIterator.hasNext()) {
            return null;
        }
        if (mNext == null) {
            int currColor = mColorIterator.next();
            mNext = new MosaicFragment(mResult.getWidth(), mResult.getHeight(), currColor);
        }
        return mNext;
    }

    @Override
    public boolean hasAll() {
        return mColorIterator != null && !mColorIterator.hasNext();
    }

    @Override
    public AbstractBitmap getReconstructed() {
        return mResult;
    }

    @Override
    public int estimatedProgressPercent() {
        int colors = mUsedColors.size();
        if (colors <= 0) {
            return 0;
        }
        return (int) (100 * mLayersApplied / (double) colors);
    }
}
