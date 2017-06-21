package data;

import data.image.ImageResolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ImageResolutionTest {

    @Test
    void testSquareResolution() {
        ImageResolution res1 = new ImageResolution(3, 3);
        assertEquals(ImageResolution.SQUARE, res1);
        ImageResolution res2 = new ImageResolution(40, 40);
        assertEquals(ImageResolution.SQUARE, res2);
    }

    @Test
    void test16To9Resolution() {
        assertEquals(new ImageResolution(16, 9), new ImageResolution(96, 54));
    }
}
