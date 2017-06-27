package util.image;

/**
 * Very basic functional interface that basically is the general function function interface from Integer to Integer
 * but without the wrapping.
 * Created by dd on 24.06.17.
 */
@FunctionalInterface
interface IntToIntFunction {

    /**
     * Applies the given parameter to the function and returns the integer result.
     * @param param The parameter.
     * @return The result.
     */
    int apply(int param);
}
