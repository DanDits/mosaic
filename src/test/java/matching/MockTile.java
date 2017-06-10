package matching;

import data.mosaic.MosaicTile;

/**
 * Created by dd on 07.06.17.
 */
public class MockTile implements MosaicTile<String> {
    private final int average;
    private final String source;
    private final int height;
    private final int width;

    public MockTile(String source, int average, int width, int height) {
        this.source = source;
        this.average = average;
        this.width = width;
        this.height = height;
    }
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public int getAverageARGB() {
        return average;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
