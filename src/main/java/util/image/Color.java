package util.image;

/**
 * Created by dd on 03.06.17.
 */
public class Color {

    public static final int TRANSPARENT = 0x00000000;
    private static final char DECIMAL_COLOR_SEPARATOR = ',';

    private Color() {}
    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int rgb(int red, int green, int blue) {
        // 0xFF << 24 technically does not fit into integer as the leftmost bit is the sign bit
        //noinspection NumericOverflow
        return (0xFF << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }


    private static final IntToIntFunction[] RGB_EXTRACTORS = new IntToIntFunction[] {Color::red,
            Color::green, Color::blue};
    private static final IntToIntFunction[] ARGB_EXTRACTORS = new IntToIntFunction[] {Color::alpha,
            Color::red, Color::green, Color::blue};
    static final IntToIntFunction[] RGBA_EXTRACTORS = new IntToIntFunction[] {Color::red,
                    Color::green, Color::blue, Color::alpha};

    public static String visualize(int argb) {
        return visualize(argb, true, false);
    }

    public static String visualize(int argb, boolean useAlpha, boolean decimalValues) {
        StringBuilder builder = new StringBuilder(decimalValues ? (useAlpha ? 15 : 11) : (useAlpha ? 10 : 8));
        IntToIntFunction[] extractors = (useAlpha ? ARGB_EXTRACTORS : RGB_EXTRACTORS);
        if (!decimalValues) {
            builder.append("0x");
        }
        for (IntToIntFunction extractor : extractors) {
            int extractedValue = extractor.apply(argb);
            if (decimalValues) {
                if (extractor != extractors[0]) {
                    builder.append(DECIMAL_COLOR_SEPARATOR);
                }
                builder.append(extractedValue);
            } else {
                // we don't simply use toHexString on total argb as we want to properly show leading zeros
                String hexValue = Integer.toHexString(extractedValue).toUpperCase();
                if (hexValue.length() == 1) {
                    builder.append('0');
                }
                builder.append(hexValue);
            }
        }
        return builder.toString();
    }


}
