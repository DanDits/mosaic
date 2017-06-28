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

package data.storage;


import data.image.ImageResolution;
import util.image.Colorized;

/**
 * This interface is a reference of anything than can be used to reconstruct a mosaic and serve as a MosaicFragment.
 * Therefore it provides an average color and the source.
 * @author Daniel
 *
 */
public interface MosaicTile<S> extends Colorized {

	@Override
	default int getColor() {
		return getAverageARGB();
	}
	
	/**
	 * Returns the source. This is anything that can reference
     * something to be turned into a MosaicFragment like a bitmap, an image
     * or a file.
	 * @return The source.
	 */
    S getSource();
	
	/**
	 * Returns the average ARGB color of the images references
	 * by this mosaic tile.
	 * @return The average ARGB color.
	 */
	int getAverageARGB();

    /**
     * Returns the width of the image referenced. Can be zero if unknown.
     * @return The width.
     */
	int getWidth();

    /**
     * Returns the height of the image referenced. Can be zero if unknown.
     * @return The height.
     */
	int getHeight();

	default ImageResolution getResolution() {
	    return new ImageResolution(getWidth(), getHeight());
    }

}