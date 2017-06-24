package util.image;

/**
 * Created by dd on 23.06.17.
 */
public abstract class ColorSpace {

    public abstract double getValue(int argb, int axis);
    public abstract double getMinimum(int axis);
    public abstract double getMaximum(int axis);
    public abstract boolean usesAlpha();
    public abstract int getDimension();

    public abstract ColorMetric getMetric();

    public double getDistance(int color1, int color2) {
        return getMetric().getDistance(color1, color2, usesAlpha());
    }

    public double getDistance(int color1, int color2, int axis) {
        return getMetric().getDistance(color1, color2, axis);
    }

    void checkAxis(int axis) {
        if (axis < 0 || axis >= getDimension()) {
            throw new IllegalArgumentException("Illegal axis: " + axis + " for dimension " + getDimension());
        }
    }

    public abstract ColorSpace getInstanceByAlpha(boolean useAlpha);

    public static class RgbAbsolute extends RgbEuclid {

        public static final ColorSpace INSTANCE_WITH_ALPHA = new RgbAbsolute(true);
        public static final ColorSpace INSTANCE_WITHOUT_ALPHA = new RgbAbsolute(false);

        private RgbAbsolute(boolean useAlpha) {
            super(useAlpha);
        }

        @Override
        public ColorMetric getMetric() {
            return ColorMetric.Absolute.INSTANCE;
        }

        @Override
        public ColorSpace getInstanceByAlpha(boolean useAlpha) {
            if (useAlpha) {
                return INSTANCE_WITH_ALPHA;
            }
            return INSTANCE_WITHOUT_ALPHA;
        }

    }

    public static class RgbEuclid extends ColorSpace {

        public static final ColorSpace INSTANCE_WITH_ALPHA = new RgbEuclid(true);
        public static final ColorSpace INSTANCE_WITHOUT_ALPHA = new RgbEuclid(false);
        private final boolean useAlpha;

        private RgbEuclid(boolean useAlpha) {
            this.useAlpha = useAlpha;
        }

        @Override
        public double getValue(int color, int axis) {
            checkAxis(axis);
            switch (axis) {
                case 0:
                    return Color.red(color);
                case 1:
                    return Color.green(color);
                case 2:
                    return Color.blue(color);
                case 3:
                    return Color.alpha(color);
                default:
                    throw new IllegalArgumentException("Illegal axis " + axis);
            }
        }

        @Override
        public double getMinimum(int axis) {
            return 0;
        }

        @Override
        public double getMaximum(int axis) {
            return 255;
        }

        @Override
        public boolean usesAlpha() {
            return useAlpha;
        }

        @Override
        public int getDimension() {
            return useAlpha ? 4 : 3;
        }

        @Override
        public ColorMetric getMetric() {
            return ColorMetric.Euclid2.INSTANCE;
        }

        @Override
        public ColorSpace getInstanceByAlpha(boolean useAlpha) {
            if (useAlpha) {
                return INSTANCE_WITH_ALPHA;
            }
            return INSTANCE_WITHOUT_ALPHA;
        }
    }


    public static class Brightness extends ColorSpace {

        public static final ColorSpace INSTANCE_WITH_ALPHA = new Brightness(true);
        public static final ColorSpace INSTANCE_WITHOUT_ALPHA = new Brightness(false);
        private final boolean useAlpha;

        private Brightness(boolean useAlpha) {
            this.useAlpha = useAlpha;
        }

        @Override
        public double getValue(int color, int axis) {
            checkAxis(axis);
            if (useAlpha) {
                return ColorAnalysisUtil.getBrightnessWithAlpha(color);
            }
            return ColorAnalysisUtil.getBrightnessNoAlpha(color);
        }

        @Override
        public double getMinimum(int axis) {
            return 0.;
        }

        @Override
        public double getMaximum(int axis) {
            return 1.;
        }

        @Override
        public boolean usesAlpha() {
            return useAlpha;
        }

        @Override
        public int getDimension() {
            return 1;
        }

        @Override
        public ColorMetric getMetric() {
            if (useAlpha) {
                return ColorMetric.BrightnessWithAlpha.INSTANCE;
            }
            return ColorMetric.BrightnessNoAlpha.INSTANCE;
        }

        @Override
        public ColorSpace getInstanceByAlpha(boolean useAlpha) {
            if (useAlpha) {
                return INSTANCE_WITH_ALPHA;
            }
            return INSTANCE_WITHOUT_ALPHA;
        }
    }
}
