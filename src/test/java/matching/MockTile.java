package matching;

import data.MosaicTile;

/**
 * Created by dd on 07.06.17.
 */
public class MockTile implements MosaicTile<String> {
    private final int average;
    private final String source;

    public MockTile(String source, int average) {
        this.source = source;
        this.average = average;
    }
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public int getAverageARGB() {
        return average;
    }
}
