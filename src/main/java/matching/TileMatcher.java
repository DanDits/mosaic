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


import data.mosaic.MosaicTile;
import reconstruction.MosaicFragment;
import util.caching.Cachable;
import util.caching.LruCache;
import util.image.ColorSpace;

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
	private static final int CACHE_SIZE = 64;
    private static final ColorSpace DEFAULT_COLOR_SPACE = ColorSpace.RgbEuclid.INSTANCE_WITHOUT_ALPHA;
	public static final int REUSE_UNLIMITED = -1;
	public static final int REUSE_NONE = 0;

	protected ColorSpace space;
    private Cachable<MosaicFragment, MosaicTile<S>> matchesCache = new LruCache<>(CACHE_SIZE);
    private Map<S, Integer> reuseCount = new HashMap<>();

	private int reuseLimit = REUSE_UNLIMITED;

	protected void setCacheSize(int size) {
    	matchesCache.setCacheSize(size);
	}

    protected void resetHashMatches() {
        matchesCache.clearCache(Cachable.CLEAR_EMPTY);
    }

    protected boolean cacheEnabled() {
		return true;
	}

    private Optional<? extends MosaicTile<S>> getBestMatchHashed(MosaicFragment wantedFragment) {
		if (cacheEnabled()) {
			return matchesCache.getFromCache(wantedFragment, this::calculateBestMatch);
		}
		return calculateBestMatch(wantedFragment);
    }

    public void setTileReuseLimit(int limit) {
    	this.reuseLimit = limit;
	}

    public final void setUseAlpha(boolean useAlpha) {
    	boolean oldUseAlpha = usesAlpha();
    	space = space.getInstanceByAlpha(useAlpha);
        if (oldUseAlpha != usesAlpha()) {
        	resetHashMatches();
        	onColorSpaceChanged();
		}
    }

	protected abstract void onColorSpaceChanged();

    protected TileMatcher(ColorSpace space) {
		this.space = space;
		if (space == null) {
			this.space = DEFAULT_COLOR_SPACE;
		}
	}

	protected abstract Optional<? extends MosaicTile<S>> calculateBestMatch(MosaicFragment wantedTile);

    public Optional<? extends MosaicTile<S>> getBestMatch(MosaicFragment wantedFragment) {
		Optional<? extends MosaicTile<S>> result;
		boolean canUseResult;
		do {
			canUseResult = true;
			result = getBestMatchHashed(wantedFragment);
			if (result.isPresent() && reuseLimit >= 0) {
				int currentReuseCount = reuseCount.getOrDefault(result.get().getSource(), -1);
				reuseCount.put(result.get().getSource(), currentReuseCount + 1);
				if (currentReuseCount >= reuseLimit) {
					canUseResult = false;
					removeTile(result.get());
					matchesCache.removeFromCache(wantedFragment);
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
	public final boolean usesAlpha() {
		return space.usesAlpha();
	}

    public final void setColorSpace(ColorSpace newSpace) {
		ColorSpace oldSpace = space;
	    if (newSpace == null) {
	        space = DEFAULT_COLOR_SPACE;
        } else {
	        space = newSpace;
        }
        if (!space.equals(oldSpace)) {
	    	resetHashMatches();
	    	onColorSpaceChanged();
		}
    }
}
