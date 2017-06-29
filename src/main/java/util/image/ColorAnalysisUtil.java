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

package util.image;

import data.image.AbstractBitmap;

/**
 * This class is an utility class. It offers RGB convertion
 * methods and helping methods to compare and store the rgb data.
 * @author Daniel
 *
 */
public final class ColorAnalysisUtil {

	
	private ColorAnalysisUtil() {
	}

	public static int interpolateAbstractColorLinear(int fromAbstractColor, int toAbstractColor, float fraction) {
		float antiFraction = 1.f - fraction;
		return Color.argb((int) ((Color.alpha(toAbstractColor) * fraction + Color.alpha(fromAbstractColor) * antiFraction)),
                          (int) ((Color.red(toAbstractColor) * fraction + Color.red(fromAbstractColor) * antiFraction)),
                          (int) ((Color.green(toAbstractColor) * fraction + Color.green(fromAbstractColor) * antiFraction)),
                          (int) ((Color.blue(toAbstractColor) * fraction + Color.blue(fromAbstractColor) * antiFraction)));
	}

	public static int colorMultiples(int color, float multiple) {
		return Color.argb((int) (Color.alpha(color) * multiple),
                          (int) (Color.red(color) * multiple),
                          (int) (Color.green(color) * multiple),
                          (int) (Color.blue(color) * multiple));
	}

    public static double factorToSimilarityBound(double factor) {
        // makes the factor in range [0,1]
        double inBoundFactor = Math.max(0.0, Math.min(1.0, factor));
        // uses a function to transform the given value to a more fitting result.
        // For the in bound merge factor x it evaluates to : f(x)= e^(a*x^b)-1
        // with a=Log(13/10) and b = Log(a/Log(101/100))/Log(2) (makes a maximum for f(1)=0.3
        // , it is f(0.5)=0.01 and the strictly monotonic ascending behavior of the e-function
        return Math.pow(Math.E, 0.26236 * Math.pow(inBoundFactor, 4.72068)) - 1.0;
    }
	
	/**
	 * Mixes the given rgb values. To take the size of the underlying
	 * picture in account, the pixel amount needs to be supplied.
	 * @param rgb1 The first RGB value.
	 * @param rgb2 The second RGB value.
	 * @param pixels1 The pixels of the image the first RGB value refers to.
	 * @param pixels2 The pixels of the image the second RGB value refers to.
	 * @return The mixed RGBA value or <code>-1</code> if a pixels value is lower than zero.
	 */
	public static int mix(int rgb1, int rgb2, int pixels1, int pixels2) {
		int red;
		int green;
		int blue;
		int alpha;
		long currColValue1;
		long currColValue2;
		long totalPixels = pixels1 + pixels2;
		if (pixels1 < 0 || pixels2 < 0) {
			return -1;
		}
		long newCol;
		//red
		currColValue1 = Color.red(rgb1);
		currColValue2 = Color.red(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		red = (int) newCol;
		//green
		currColValue1 = Color.green(rgb1);
		currColValue2 = Color.green(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		green = (int) newCol;
		//blue
		currColValue1 = Color.blue(rgb1);
		currColValue2 = Color.blue(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		blue = (int) newCol;
		//alpha
		currColValue1 = Color.alpha(rgb1);
		currColValue2 = Color.alpha(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		alpha = (int) newCol;
		return Color.argb(alpha, red, green, blue);
	}

	public static int getAverageColor(AbstractBitmap image) {
		int width = image.getWidth();
		int height = image.getHeight();
		long averageRed = 0, averageGreen = 0, averageBlue = 0, averageAlpha = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgba = image.getPixel(x, y);
				averageRed += Color.red(rgba);
				averageGreen += Color.green(rgba);
				averageBlue += Color.blue(rgba);
				averageAlpha += Color.alpha(rgba);
			}
		}
		long pixels = width * height;
		return Color.argb((int) (averageAlpha / pixels), (int) (averageRed / pixels), (int) (averageGreen / pixels), (int) (averageBlue / pixels));
	}


    @FunctionalInterface
	public interface ColoredCoordinates {
		int getColorOfCoordinates(int x, int y);
	}

	public static double[] getAverageValues(ColoredCoordinates colorCoords, int fromX, int toX, int fromY, int toY,
                                      ColorSpace space) {
        double[] average = new double[space.getDimension()];
        for (int x = fromX; x < toX; x++) {
            for (int y = fromY; y < toY; y++) {
                int argb = colorCoords.getColorOfCoordinates(x, y);
                for (int i = 0; i < space.getDimension(); i++) {
                    average[i] += space.getValue(argb, i);
                }
            }
        }
        long pixels = (toX - fromX) * (toY - fromY);
        pixels = pixels <= 0L ? 1 : pixels; // ensure no division by zero
        for (int i = 0; i < space.getDimension(); i++) {
            average[i] /= pixels;
        }
        return average;
    }

	public static int getAverageColor(ColoredCoordinates colorCoords, int fromX, int toX, int fromY, int toY,
	                                  ColorSpace space) {
        return space.valuesToArgb(getAverageValues(colorCoords, fromX, toX, fromY, toY, space));
    }

	public static int getAverageColor(ColoredCoordinates colorCoords, int fromX, int toX, int fromY, int toY) {
        return getAverageColor(colorCoords, fromX, toX, fromY, toY, ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA);
    }

	public static int getAverageColor(AbstractBitmap image, int fromX, int toX, int fromY, int toY) {
		return getAverageColor(image::getPixel, fromX, toX, fromY, toY);
	}
	
	
	/**
	 * Calculates the brightness of the given rgb value using the alpha channel as
	 * a human would recognize it. Note that a fully transparent color would be considered to be bright.
	 * @param rgb The rgb value
	 * @return The brightness where 1 is very bright and 0 is very dark.
	 */
	public static double getBrightnessWithAlpha(int rgb) {
        // formula: (255-alpha)/255 + alpha*(0.299*red+0.587*green+0.114*blue)/(255*255)
        // white 255/255/255 is always considered to be very bright(=1), no matter the alpha
        return 1. +((rgb >> 24) & 0xFF) * (-1./255. + 0.299/65025.0 * ((rgb >> 16) & 0xFF) + 0.587/65025.0 * ((rgb >> 8) & 0xFF) + 0.114/65025.0 * (rgb & 0xFF));
	}

	public static int getArgbForBrightness(double brightness) {
	    // there is no unique solution to (0.299*red+0.587*green+0.114*blue)/255=brightness
        // so we assume g=red=green=blue and solve for g which is g=brightness*255
	    int greyness = (int) (brightness * 255);
	    return Color.rgb(greyness, greyness, greyness);
    }

    /**
     * Calculates the brightness of the given rgb value as a human would recognize it.
     * @param rgb The rgb value.
     * @return The brightness where 1 is very bright and 0 is very dark.
     */
    public static double getBrightnessNoAlpha(int rgb) {
        // formula: (0.299*red+0.587*green+0.114*blue)/255
        return 0.299/255. * ((rgb >> 16) & 0xFF) + 0.587/255. * ((rgb >> 8) & 0xFF) + 0.114/255. * (rgb & 0xFF);
    }
	
	/**
	 * Calculates the "greyness" of the given RGB color, which is a way
	 * to measure the distance from the grey colors with red=green=blue.
	 * @param red The red value.
	 * @param green The green value.
	 * @param blue The blue value.
	 * @return Greyness of 0 means that red=green=blue which is the most grey and 1 means
     * the least grey, like (255,0,0).
	 */
	public static double getGreyness(int red, int green, int blue) {
		double mean = (red + green + blue) / 3.0;
		// square of the euclid distance from the straight line from 0/0/0 to 255/255/255 divided by the maximum possible distance
        // which is (255/3-255)²+2*(255/3)² for a color in the corner of the color square
		return ((mean - red) * (mean - red) + (mean - blue) * (mean - blue) + (mean - green) * (mean - green)) / 43350.;
	}
}
