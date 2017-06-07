package ui;

import data.MosaicTile;

import java.io.File;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicTile implements MosaicTile<String> {

    private int averageARGB;
    private String path;

    public FileMosaicTile(String path) {
        this.path = path;
    }

    FileMosaicTile(String path, int averageARGB) {
        this.path = path;
        this.averageARGB = averageARGB;
    }

    @Override
    public String getSource() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMosaicTile that = (FileMosaicTile) o;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int getAverageARGB() {
        return averageARGB;
    }
}
