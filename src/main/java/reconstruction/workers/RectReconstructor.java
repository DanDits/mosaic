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
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import reconstruction.MosaicFragment;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import util.image.ColorAnalysisUtil;

/**
 * This class models a specific {@link Reconstructor} which fragments
 * an image into rectangulars of equal height and width. This is a fast
 * and lightweight reconstruction, the default mosaic technique. As this
 * asserts all rects to have equal height and equal width, the given values
 * may be adjusted for the given image.
 * @author Daniel
 *
 */
public class RectReconstructor extends Reconstructor {
	protected final int mRectHeight;
	protected final int mRectWidth;
	protected int[][] resultingRGBA;
	private AbstractBitmap result;
    protected AbstractCanvas mResultCanvas;
	private int nextImageIndex;

	public static class RectParameters extends ReconstructionParameters{
		public int wantedRows;
		public int wantedColumns;

		public RectParameters(AbstractBitmap source) {
			super(source);
		}

		@Override
		public Reconstructor makeReconstructor() throws IllegalParameterException {
			return new RectReconstructor(this);
		}

		@Override
		protected void resetToDefaults() {
			wantedRows = wantedColumns = 20;
		}

		@Override
		protected void validateParameters() throws IllegalParameterException {
			if (wantedRows <= 0) {
				throw new IllegalParameterException(wantedRows, "Rows must be positive.");
			}
			if (wantedColumns <= 0) {
				throw new IllegalParameterException(wantedColumns, "Columns must be positive.");
			}
		}
	}

	public RectReconstructor(RectParameters parameters) throws ReconstructionParameters.IllegalParameterException {
		if (parameters == null) {
			throw new NullPointerException();
		}
		parameters.validateParameters();
		AbstractBitmap source = parameters.getBitmapSource();
		int actualRows = Reconstructor.getClosestCount(source.getHeight(), parameters.wantedRows);
		int actualColumns = Reconstructor.getClosestCount(source.getWidth(), parameters.wantedColumns);
		this.mRectHeight = source.getHeight() / actualRows;
		this.mRectWidth = source.getWidth() / actualColumns;
		this.resultingRGBA = new int[actualRows][actualColumns];
		this.nextImageIndex = 0;
		this.result = obtainBaseBitmap(this.mRectWidth * this.getColumns(), this.mRectHeight * this.getRows());
        mResultCanvas = AbstractCanvasFactory.getInstance().makeCanvas(result);
        evaluateResultingRGBA(source, actualRows, actualColumns);
	}

	private void evaluateResultingRGBA(AbstractBitmap source, int rows, int columns) {
        // evaluate the fragments average colors
        for (int heightIndex = 0; heightIndex < rows; heightIndex++) {
            for (int widthIndex = 0; widthIndex < columns; widthIndex++) {
                this.resultingRGBA[heightIndex][widthIndex]
                        = evaluateRectValue(source, widthIndex * this.mRectWidth,
                        (widthIndex + 1) * this.mRectWidth,
                        heightIndex * this.mRectHeight,
                        (heightIndex + 1) * this.mRectHeight);
            }
        }
    }

    protected int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        return ColorAnalysisUtil.getAverageColor(source, startX, endX, startY, endY);
    }
	
	/**
	 * The amount of rows of the fragmenation.
	 * @return The amount of rows of the fragmentation. Greater than or equal 1.
	 */
	protected int getRows() {
		return this.resultingRGBA.length;
	}
	
	/**
	 * Returns the amount of columns of the fragmentation.
	 * @return The amount of columns of the fragmentation. Greater than or equal 1.
	 */
	protected int getColumns() {
		return this.resultingRGBA[0].length;
	}
	
	@Override
	public boolean giveNext(AbstractBitmap nextFragmentImage) {
		if (!this.hasAll()
				&& nextFragmentImage != null 
				&& nextFragmentImage.getWidth() == this.mRectWidth
				&& nextFragmentImage.getHeight() == this.mRectHeight) {

            mResultCanvas.drawBitmap(nextFragmentImage,
                    (this.nextImageIndex % this.getColumns()) * this.mRectWidth,
                    (this.nextImageIndex / this.getColumns()) * this.mRectHeight);
			this.nextImageIndex++;
			return true;
		}
		return false;
	}

	@Override
	public MosaicFragment nextFragment() {
		if (this.hasAll()) {
			return null;
		}
		return new MosaicFragment(this.mRectWidth, this.mRectHeight,
				this.resultingRGBA[this.nextImageIndex / this.getColumns()]
						[this.nextImageIndex % this.getColumns()]);
	}

	@Override
	public boolean hasAll() {
		return this.nextImageIndex >= this.getRows() * this.getColumns();
	}

	@Override
	public AbstractBitmap getReconstructed() {
		if (this.hasAll()) {
			AbstractBitmap temp = this.result;
			this.result = null;
			return temp;
		}
		return null;
	}

    @Override
    public int estimatedProgressPercent() {
        return (int) (100 * nextImageIndex / (double) (getRows() * getColumns()));
    }
}
