package data.image;

/**
 * Created by dd on 03.06.17.
 */
public class AbstractColor {

    public static final int TRANSPARENT = 0x00000000;

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int rgb(int red, int green, int blue) {
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
}
