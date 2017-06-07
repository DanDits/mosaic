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

package matching;



import util.caching.Cachable;
import util.caching.LruCache;
import util.image.ColorMetric;
import data.MosaicTile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class models an abstract TileMatcher which can with a certain
 * accuracy give the best matching mosaic tile to a given rgb color.
 * @author Daniel
 *
 */
public abstract class TileMatcher<S> {
	//TODO add way to filter tiles before calculating best match (or using the hashed) to filter for example to (almost) fitting aspect ratio
	private static final int CACHE_SIZE = 64;
    private static final ColorMetric DEFAULT_COLOR_METRIC = ColorMetric.Euclid2.INSTANCE;
	public static final int REUSE_UNLIMITED = -1;
	public static final int REUSE_NONE = 0;

	ColorMetric mColorMetric;
    private Cachable<Integer, MosaicTile<S>> mMatchesCache = new LruCache<>(CACHE_SIZE);
    private Map<S, Integer> reuseCount = new HashMap<>();

    /**
     * If the TileMatcher uses the alpha value of the rgb values for matching.
     */
    boolean useAlpha;
	private int reuseLimit = REUSE_UNLIMITED;

	protected void setCacheSize(int size) {
    	mMatchesCache.setCacheSize(size);
	}

    void resetHashMatches() {
        mMatchesCache.clearCache(Cachable.CLEAR_EMPTY);
    }

    private Optional<? extends MosaicTile<S>> getBestMatchHashed(int color) {
        return mMatchesCache.getFromCache(color, this::calculateBestMatch);
    }

    public void setTileReuseLimit(int limit) {
    	this.reuseLimit = limit;
	}

    public void setUseAlpha(boolean useAlpha) {
    	boolean oldUseAlpha = this.useAlpha;
        this.useAlpha = useAlpha;
        if (oldUseAlpha != useAlpha) {
        	resetHashMatches();
		}
    }

	/**
	 * Creates a new tile matcher, the given flag simply signals
	 * if the implementing tile matcher uses alpha for matching.
     * @param useAlpha If the matcher uses the alpha value for matching.
     * @param metric The color metric to use, if null defaults to Euclid2.
     */
    TileMatcher(boolean useAlpha, ColorMetric metric) {
		setUseAlpha(useAlpha);
		setColorMetric(metric);
	}

	/**
	 * Returns the best matching MosaicTile for the given rgb color.
	 * The result and speed of this calculation highly depends on the tile matcher.
	 * Higher accuracy usually returns better results at a cost of speed.
	 * @param withRGB The rgb to match. If the alpha value is used can be requested by
	 * usesAlpha().
	 * @return The best matching mosaic tile. If the tile matcher has tile data
	 * this will never be <code>null</code>.
	 */
	protected abstract Optional<? extends MosaicTile<S>> calculateBestMatch(int withRGB);

    public Optional<? extends MosaicTile<S>> getBestMatch(int color) {
		Optional<? extends MosaicTile<S>> result;
		boolean canUseResult;
		do {
			canUseResult = true;
			result = getBestMatchHashed(color);
			if (result.isPresent() && reuseLimit >= 0) {
				int currentReuseCount = reuseCount.getOrDefault(result.get().getSource(), -1);
				reuseCount.put(result.get().getSource(), currentReuseCount + 1);
				if (currentReuseCount >= reuseLimit) {
					canUseResult = false;
					removeTile(result.get());
					mMatchesCache.removeFromCache(color);
				}
			}
		} while (!canUseResult);
		return result;
    }

	/**
	 * Returns the accuracy of the best match. 
	 * @return A value from <code>0</code> (bad accuracy) to
	 * <code>1</code> (best result).
	 */
	public abstract double getAccuracy();
    public abstract boolean setAccuracy(double accuracy);
	
	/**
	 * Removes one occurance of the given MosaicTile from the TileMatcher. This operation can
	 * be performed during matching and is useful to eleminate tiles which reference
	 * an invalid image file (which got deleted or is unaccesable) or MosaicTiles that should not be used
	 * anymore for any other reason.
	 * @param toRemove The MosaicTile to remove.
	 * @return <code>true</code> only if the tile was contained in the set of MosaicTiles
	 * of this matcher and then removed.
	 */
	public abstract boolean removeTile(MosaicTile<S> toRemove);
	
	/**
	 * Returns the amount of MosaicTiles used by this TileMatcher.
	 * @return The amount of MosaicTiles used by this TileMatcher.
	 */
	public abstract int getUsedTilesCount();
	
	/**
	 * Returns <code>true</code> if calculateBestMatch() takes the alpha
	 * value of the rgb color codes into account.
	 * @return If this matcher uses alpha.
	 */
	public boolean usesAlpha() {
		return this.useAlpha;
	}

    public void setColorMetric(ColorMetric metric) {
		ColorMetric oldMetric = mColorMetric;
	    if (metric == null) {
	        mColorMetric = DEFAULT_COLOR_METRIC;
        } else {
	        mColorMetric = metric;
        }
        if (oldMetric != mColorMetric) {
	    	resetHashMatches();
		}
    }
}
