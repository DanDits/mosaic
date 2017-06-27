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
import util.PercentProgressListener;
import util.image.ColorSpace;

import java.util.Arrays;
import java.util.Random;

/**
 * 'Better' version of AutoLayerReconstructor using the k-means algorithm. In contrast to the other layer reconstructor
 * the amount of layers needs to be fixed and given though. Results are cleaner for the same run speed
 * and similar parameter settings, though the parameter needs to be determined by the user('s preference)
 * and cannot be set to an arbitrary constant.
 * Created by daniel on 03.07.15.
 */
public class FixedLayerReconstructor extends Reconstructor {
    private static final int MAX_RECALCULATIONS_BASE = 7;
    private static final int MAX_RECALCULATIONS_LINEAR_GROWTH = 2;
    private final ColorSpace space;
    private MosaicFragment mFragment;
    private AbstractBitmap mResult;
    private int[] mPixelClusterNumber;
    private AbstractBitmap[] mClusterBitmaps;
    private int[] mClusterColors;
    private int mCurrCluster;
    private int[] mClusterBitmapOffsetX;
    private int[] mClusterBitmapWidth;
    private int[] mClusterBitmapOffsetY;
    private int[] mClusterBitmapHeight;

    public static class FixedLayerParameters extends ReconstructionParameters {
        private static final ColorSpace DEFAULT_SPACE = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        public PercentProgressListener progress;
        public ColorSpace space;
        public int layersCount;

        public FixedLayerParameters(AbstractBitmap source) {
            super(source);
        }

        @Override
        public Reconstructor makeReconstructor() throws IllegalParameterException {
            return new FixedLayerReconstructor(this);
        }

        @Override
        protected void resetToDefaults() {
            space = DEFAULT_SPACE;
            layersCount = 3;
            progress = null;
        }

        @Override
        protected void validateParameters() throws IllegalParameterException {
            if (space == null) {
                space = DEFAULT_SPACE;
            }
            if (layersCount < 1) {
                throw new IllegalParameterException(layersCount, "Layers count must be positive.");
            }
        }
    }

    private FixedLayerReconstructor(FixedLayerParameters parameters) throws ReconstructionParameters.IllegalParameterException {
        parameters.validateParameters();
        space = parameters.space;
        init(parameters.getBitmapSource(), parameters.layersCount, parameters.progress);
    }

    private void init(AbstractBitmap source, final int clusterCount, PercentProgressListener progress) {
        if (clusterCount < 1) {
            throw new IllegalArgumentException("No clusters?!" + clusterCount);
        }
        final int width = source.getWidth();
        final int height = source.getHeight();
        mResult = obtainBaseBitmap(width, height);
        mClusterBitmaps = new AbstractBitmap[clusterCount];
        int[] pixelColors = new int[width * height];
        mPixelClusterNumber = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixelColors[x + y * width] = source.getPixel(x, y);
            }
        }

        // k-means algorithm
        Random rand = new Random();

        int[] clusterCenters = new int[clusterCount];
        double[] clusterWeights = new double[clusterCount];

        /* //init (with random centers, also other possibilities as convergence greatly depends on starting values)
        for (int i = 0; i < clusterCenters.length; i++) {
            clusterCenters[i] = pixelColors[rand.nextInt(pixelColors.length)];
        }*/

        // init k-means++ style by preferring centers that are further away from the last center
        clusterCenters[0] = pixelColors[rand.nextInt(pixelColors.length)];
        final double maxDist = space.getMaxDistance();
        for (int cluster = 1; cluster < clusterCount; cluster++) {
            clusterCenters[cluster] = pixelColors[rand.nextInt(pixelColors.length)];
            for (int i = 0; i < pixelColors.length; i++) {
                double distFraction = space.getDistance(pixelColors[i], clusterCenters[cluster - 1]) / maxDist;
                distFraction *= distFraction;
                distFraction *= distFraction;
                distFraction *= i / (double) pixelColors.length;
                if (rand.nextDouble() < distFraction) {
                    clusterCenters[cluster] = pixelColors[i];
                    break;
                }
            }
        }

        int[][] valuesPerDimension = new int[space.getDimension()][clusterCount];
        int[] clusterSize = new int[clusterCount];
        int redistributionCount = 0;
        int maxRedistributions = MAX_RECALCULATIONS_BASE + MAX_RECALCULATIONS_LINEAR_GROWTH * clusterCount;
        int changed = Integer.MAX_VALUE;
        int lastChanged;
        do {
            // for improved speed we expect monotonous convergence in the amount of pixels changed, if this ever increases again we stop
            lastChanged = changed;
            changed = 0;
            // redistribute into clusters
            Arrays.fill(clusterWeights, 0.);
            for (int i = 0; i < pixelColors.length; i++) {
                int currColor = pixelColors[i];
                double minWeightIncrease = Double.MAX_VALUE;
                int minWeightIncreaseIndex = 0;
                for (int cluster = 0; cluster < clusterCount; cluster++) {
                    double currWeightIncrease = space.getDistance(clusterCenters[cluster], currColor);
                    if (currWeightIncrease < minWeightIncrease) {
                        minWeightIncrease = currWeightIncrease;
                        minWeightIncreaseIndex = cluster;
                    }
                }
                clusterWeights[minWeightIncreaseIndex] += minWeightIncrease;
                int oldNumber = mPixelClusterNumber[i];
                mPixelClusterNumber[i] = minWeightIncreaseIndex;
                if (oldNumber != minWeightIncreaseIndex) {
                    changed++;
                }
            }
            redistributionCount++;

            // recalculate centers
            for (int[] aValuesPerDimension : valuesPerDimension) {
                Arrays.fill(aValuesPerDimension, 0);
            }
            Arrays.fill(clusterSize, 0);
            for (int i = 0; i < pixelColors.length; i++) {
                int cluster = mPixelClusterNumber[i];
                int color = pixelColors[i];
                for (int j = 0; j < valuesPerDimension.length; j++) {
                    valuesPerDimension[j][cluster] += space.getValue(color, j);
                }
                clusterSize[cluster]++;
            }
            double[] valuesPerCluster = new double[valuesPerDimension.length];
            for (int cluster = 0; cluster < clusterCount; cluster++) {
                int size = clusterSize[cluster];
                if (size > 0) {
                    for (int i = 0; i < valuesPerCluster.length; i++) {
                        valuesPerCluster[i] = valuesPerDimension[i][cluster] / size;
                    }
                    clusterCenters[cluster] = space.valuesToArgb(valuesPerCluster);
                }
            }
            progress.onProgressUpdate((int) (PercentProgressListener.PROGRESS_COMPLETE * redistributionCount / (double) maxRedistributions));
        } while (changed <= lastChanged && redistributionCount < maxRedistributions);
        mClusterColors = clusterCenters;
        calculateClusterOffsets();
        progress.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
    }

    private void calculateClusterOffsets() {
        int clusterCount = mClusterBitmaps.length;
        mClusterBitmapOffsetX = new int[clusterCount];
        mClusterBitmapOffsetY = new int[clusterCount];
        mClusterBitmapWidth = new int[clusterCount];
        mClusterBitmapHeight = new int[clusterCount];
        for (int i = 0; i < clusterCount; i++) {
            calculateClusterOffset(i);
        }
    }

    private void calculateClusterOffset(int cluster) {
        int width = mResult.getWidth();
        int minRow = Integer.MAX_VALUE, maxRow = -1;
        int minCol = Integer.MAX_VALUE, maxCol = -1;
        for (int i = 0; i < mPixelClusterNumber.length; i++) {
            int x = i % width;
            int y = i / width;
            if (mPixelClusterNumber[i] == cluster) {
                minRow = Math.min(minRow, y);
                maxRow = Math.max(maxRow, y);
                minCol = Math.min(minCol, x);
                maxCol = Math.max(maxCol, x);
            }
        }
        mClusterBitmapOffsetX[cluster] = minCol;
        mClusterBitmapOffsetY[cluster] = minRow;
        mClusterBitmapWidth[cluster] = maxCol - minCol + 1;
        mClusterBitmapHeight[cluster] = maxRow - minRow + 1;
    }

    @Override
    public boolean giveNext(AbstractBitmap nextFragmentImage) {
        if (nextFragmentImage == null || hasAll() || nextFragmentImage.getWidth() != mFragment.getWidth() || nextFragmentImage.getHeight() != mFragment.getHeight()) {
            return false;
        }
        mClusterBitmaps[mCurrCluster] = nextFragmentImage;
        mCurrCluster++;
        return true;
    }

    @Override
    public MosaicFragment nextFragment() {
        if (hasAll()) {
            return null;
        }
        mFragment = new MosaicFragment(mClusterBitmapWidth[mCurrCluster], mClusterBitmapHeight[mCurrCluster], mClusterColors[mCurrCluster]);
        return mFragment;
    }

    @Override
    public boolean hasAll() {
        return mCurrCluster >= mClusterColors.length;
    }

    @Override
    public AbstractBitmap getReconstructed() {
        int width = mResult.getWidth();
        for (int i = 0; i < mPixelClusterNumber.length; i++) {
            int x = i % width;
            int y = i / width;
            int cluster = mPixelClusterNumber[i];
            int bitmapColor = mClusterBitmaps[cluster].getPixel(x - mClusterBitmapOffsetX[cluster],
                                                                y - mClusterBitmapOffsetY[cluster]);
            mResult.setPixel(x, y, bitmapColor);
        }
        return mResult;
    }

    @Override
    public int estimatedProgressPercent() {
        return (int) (100 * mCurrCluster / (double) mClusterColors.length);
    }
}
