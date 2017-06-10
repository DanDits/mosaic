package data;

import data.image.ImageResolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by daniel on 08.06.17.
 */
public class ImageResolutionTest {

    @Test
    public void testSquareResolution() {
        ImageResolution res1 = new ImageResolution(3, 3);
        assertEquals(ImageResolution.SQUARE, res1);
        ImageResolution res2 = new ImageResolution(40, 40);
        assertEquals(ImageResolution.SQUARE, res2);
    }

    @Test
    public void test16To9Resolution() {
        assertEquals(new ImageResolution(16, 9), new ImageResolution(96, 54));
    }
}
