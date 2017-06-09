package data;


import java.math.BigInteger;

/**
 * Created by daniel on 08.06.17.
 */
public class ImageResolution {
    public static final ImageResolution SQUARE = new ImageResolution(1, 1);
    private final int width;
    private final int height;

    public ImageResolution(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Illegal width or height:" + width + "/" + height);
        }
        int gcd = getGreatestCommonDivisor(width, height);
        this.width = width / gcd;
        this.height = height / gcd;
    }

    private static int getGreatestCommonDivisor(int int1, int int2) {
        return BigInteger.valueOf(int1).gcd(BigInteger.valueOf(int2)).intValue();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageResolution that = (ImageResolution) o;

        if (width != that.width) return false;
        return height == that.height;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "resolution " + width + ":" + height;
    }
}
