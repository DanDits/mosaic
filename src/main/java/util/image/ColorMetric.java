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


import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 03.07.15.
 */
public abstract class ColorMetric {

    protected abstract double getDistance(int color1, int color2, int axis);
    public abstract double getDistance(int color1, int color2, boolean useAlpha);
    public abstract double maxValue(boolean useAlpha);

    public static List<ColorMetric> makeAll() {
        List<ColorMetric> list = new ArrayList<>(8);
        list.add(Euclid2.INSTANCE);
        list.add(Absolute.INSTANCE);
        list.add(Greyness.INSTANCE);
        list.add(AbsoluteRed.INSTANCE);
        list.add(AbsoluteGreen.INSTANCE);
        list.add(AbsoluteBlue.INSTANCE);
        list.add(BrightnessWithAlpha.INSTANCE);
        list.add(BrightnessNoAlpha.INSTANCE);
        return list;
    }

    public static class Euclid2 extends ColorMetric {	/**
         * This value indicates the greatest possible value for two colors
         * if alpha is not used. Equal to 3*255*255.
         */
        private static final double GREATEST_VALUE_NO_ALPHA = 3 * 255 * 255;

        /** This value indicates the greatest possible value for two colors
         * if alpha is used. Equal to 4*255*255.
         */
        private static final double GREATEST_VALUE_ALPHA = 4 * 255 * 255;
        public static final Euclid2 INSTANCE = new Euclid2();
        private Euclid2() {}


        @Override
        protected double getDistance(int color1, int color2, int axis) {
            IntToIntFunction extractor = Color.RGBA_EXTRACTORS[axis];
            int diff = extractor.apply(color1) -extractor.apply(color2);
            return diff * diff;
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            int axisCount = useAlpha ? 4 : 3;
            int result = 0;
            for (int i = 0; i < axisCount; i++) {
                result += getDistance(color1, color2, i);
            }
            return result;
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? GREATEST_VALUE_ALPHA : GREATEST_VALUE_NO_ALPHA;
        }
        @Override
        public String toString() {
            return "Euclid2";
        }
    }

    public static class Absolute extends ColorMetric {
        public static final Absolute INSTANCE = new Absolute();
        private static final double GREATEST_VALUE_NO_ALPHA = 3 * 255;
        private static final double GREATEST_VALUE_ALPHA = 4 * 255;
        private Absolute() {}

        private static final IntToIntFunction[] AXIS_EXTRACTORS = new IntToIntFunction[] {Color::red,
                Color::green, Color::blue, Color::alpha};

        @Override
        protected double getDistance(int color1, int color2, int axis) {
            return Math.abs(AXIS_EXTRACTORS[axis].apply(color1) - AXIS_EXTRACTORS[axis].apply(color2));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            int axisCount = useAlpha ? 4 : 3;
            int result = 0;
            for (int i = 0; i < axisCount; i++) {
                result += getDistance(color1, color2, i);
            }
            return result;

        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? GREATEST_VALUE_ALPHA : GREATEST_VALUE_NO_ALPHA;
        }
        @Override
        public String toString() {
            return "Absolute";
        }
    }

    public static class Greyness extends ColorMetric {
        public static final Greyness INSTANCE = new Greyness();
        private Greyness() {}

        @Override
        protected double getDistance(int color1, int color2, int axis) {
            return Math.abs(ColorAnalysisUtil.getGreyness(Color.red(color1), Color.green(color1), Color.blue(color1))
                                    - ColorAnalysisUtil.getGreyness(Color.red(color2), Color.green(color2), Color.blue(color2)));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return getDistance(color1, color2, 0);
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return 1.0;
        }
        @Override
        public String toString() {
            return "Greyness";
        }
    }

    public static class AbsoluteRed extends ColorMetric {
        public static final AbsoluteRed INSTANCE = new AbsoluteRed();
        private AbsoluteRed() {}

        @Override
        protected double getDistance(int color1, int color2, int axis) {
            if (axis == 0) {
                return Math.abs(Color.red(color1) - Color.red(color2));
            }
            return  Math.abs(Color.alpha(color1) - Color.alpha(color2));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return getDistance(color1, color2, 0) + (useAlpha ? getDistance(color1, color2, 1): 0);
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? 255 * 2 : 255;
        }

        @Override
        public String toString() {
            return "AbsolutRed";
        }
    }


    public static class AbsoluteGreen extends ColorMetric {
        public static final AbsoluteGreen INSTANCE = new AbsoluteGreen();
        private AbsoluteGreen() {}
        @Override
        protected double getDistance(int color1, int color2, int axis) {
            if (axis == 0) {
                return Math.abs(Color.green(color1) - Color.green(color2));
            }
            return  Math.abs(Color.alpha(color1) - Color.alpha(color2));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return getDistance(color1, color2, 0) + (useAlpha ? getDistance(color1, color2, 1): 0);
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? 255 * 2 : 255;
        }

        @Override
        public String toString() {
            return "AbsolutGreen";
        }
    }

    public static class AbsoluteBlue extends ColorMetric {
        public static final AbsoluteBlue INSTANCE = new AbsoluteBlue();
        private AbsoluteBlue() {}
        @Override
        protected double getDistance(int color1, int color2, int axis) {
            if (axis == 0) {
                return Math.abs(Color.blue(color1) - Color.blue(color2));
            }
            return  Math.abs(Color.alpha(color1) - Color.alpha(color2));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return getDistance(color1, color2, 0) + (useAlpha ? getDistance(color1, color2, 1): 0);
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? 255 * 2 : 255;
        }

        @Override
        public String toString() {
            return "AbsolutBlue";
        }
    }

    public static class BrightnessWithAlpha extends  ColorMetric {
        public static final BrightnessWithAlpha INSTANCE = new BrightnessWithAlpha();
        private BrightnessWithAlpha() {}

        @Override
        protected double getDistance(int color1, int color2, int axis) {
            return  Math.abs(ColorAnalysisUtil.getBrightnessWithAlpha(color1) - ColorAnalysisUtil.getBrightnessWithAlpha(color2));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return getDistance(color1, color2, 0);
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return 1.0;
        }

        @Override
        public String toString() {
            return "BrightnessWithAlpha";
        }
    }

    public static class BrightnessNoAlpha extends  ColorMetric {
        public static final BrightnessNoAlpha INSTANCE = new BrightnessNoAlpha();
        private BrightnessNoAlpha() {}

        @Override
        protected double getDistance(int color1, int color2, int axis) {
            return Math.abs(ColorAnalysisUtil.getBrightnessNoAlpha(color1) - ColorAnalysisUtil.getBrightnessNoAlpha(color2));
        }

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return getDistance(color1, color2, 0);
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return 1.0;
        }

        @Override
        public String toString() {
            return "BrightnessWithoutAlpha";
        }
    }

}
