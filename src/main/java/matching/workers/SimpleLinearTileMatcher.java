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

package matching.workers;


import data.mosaic.MosaicTile;
import matching.TileMatcher;
import reconstruction.MosaicFragment;
import util.image.ColorSpace;

import java.util.*;

/**
 * This class implements a {@link TileMatcher}. This is a very basic
 * TileMatcher, very slow (no caching) in O(n) with n being the amount
 * of tiles given in the database. But maybe useful if you create
 * a new Matcher and want to check the results...<br>
 * Has accuracy 1.0
 * @author Daniel
 *
 */
public class SimpleLinearTileMatcher<S> extends TileMatcher<S> {
	private List<MosaicTile<S>> tiles;

	public SimpleLinearTileMatcher(Collection<? extends MosaicTile<S>> data, ColorSpace space) {
		super(space);
		this.tiles = new LinkedList<>(data);
	}

	@Override
	protected void onColorSpaceChanged() {
		// ignore
	}

	@Override
	public Optional<MosaicTile<S>> calculateBestMatch(MosaicFragment fragment) {
		return this.tiles.stream().min(Comparator.comparingDouble(
											tile -> space.getDistance(tile.getAverageARGB(), fragment.getAverageRGB())));
	}

	@Override
	public double getAccuracy() {
		return 1.0;
	}

	@Override
	public boolean setAccuracy(double accuracy) {
		return false;
	}

	@Override
	public boolean removeTile(MosaicTile toRemove) {
		return this.tiles.remove(toRemove);
	}

	@Override
	public int getUsedTilesCount() {
		return this.tiles.size();
	}

}
