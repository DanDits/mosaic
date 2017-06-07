package data;

import java.io.File;
import java.io.IOException;

/**
 * Created by dd on 03.06.17.
 */
public interface AbstractBitmap {

    int getWidth();
    int getHeight();
    int getPixel(int x, int y);
    void setPixel(int x, int y, int argb);

    void resize(int width, int height);

    boolean saveToFile(File file) throws IOException;

    AbstractBitmap obtainResized(int width, int height);

}
