package util;

import util.image.Color;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class AbstractColorTest {

    @Test
    public void testArgb() {
        int argb = Color.argb(0xFA, 0x13, 0xF1, 0x24);
        assertEquals(0xFA13F124, argb);
        argb = Color.argb(0xFF, 0x00, 0xF1, 0x00);
        assertEquals(0xFF00F100, argb);
    }

    @Test
    public void testRgb() {
        int argb = Color.rgb(0x13, 0xF1, 0x24);
        assertEquals(0xFF13F124, argb);
        argb = Color.rgb(0xFF, 0xFF, 0x00);
        assertEquals(0xFFFFFF00, argb);
    }

    @Test
    public void testIndividualColors() {
        int argb = 0xFA13F124;
        assertEquals(0xFA, Color.alpha(argb));
        assertEquals(0x13, Color.red(argb));
        assertEquals(0xF1, Color.green(argb));
        assertEquals(0x24, Color.blue(argb));
    }

    @Test
    public void testVisualizeColor() {
        assertEquals("0xFFFFFFFF", Color.visualize(0xFFFFFFFF, true, false));
        assertEquals("255,255,255,255", Color.visualize(0xFFFFFFFF, true, true));
        assertEquals("0xFFFFFF", Color.visualize(0xFFFFFFFF, false, false));
        assertEquals("255,255,255", Color.visualize(0xFFFFFFFF, false, true));
        assertEquals("0xABCD1204", Color.visualize(0xABCD1204, true, false));
        assertEquals("122,133,50,0", Color.visualize(Color.argb(122, 133, 50, 0),
                                                     true, true));
    }

}
