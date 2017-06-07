package util;

import data.AbstractColor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Created by dd on 03.06.17.
 */
public class AbstractColorTest {

    @Test
    public void testArgb() {
        int argb = AbstractColor.argb(0xFA, 0x13, 0xF1, 0x24);
        assertEquals(0xFA13F124, argb);
        argb = AbstractColor.argb(0xFF, 0x00, 0xF1, 0x00);
        assertEquals(0xFF00F100, argb);
    }

    @Test
    public void testRgb() {
        int argb = AbstractColor.rgb(0x13, 0xF1, 0x24);
        assertEquals(0xFF13F124, argb);
        argb = AbstractColor.rgb(0xFF, 0xFF, 0x00);
        assertEquals(0xFFFFFF00, argb);
    }

    @Test
    public void testIndividualColors() {
        int argb = 0xFA13F124;
        assertEquals(0xFA, AbstractColor.alpha(argb));
        assertEquals(0x13, AbstractColor.red(argb));
        assertEquals(0xF1, AbstractColor.green(argb));
        assertEquals(0x24, AbstractColor.blue(argb));
    }


}
