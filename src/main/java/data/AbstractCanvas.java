package data;


/**
 * Created by dd on 03.06.17.
 */
public interface AbstractCanvas {

    void drawBitmap(AbstractBitmap bitmap, int x, int y);

    void clear();

    void drawColor(int color);

    void drawCircle(int centerX, int centerY, int radius, int color);

    void drawMultiplicativly(AbstractBitmap image);
}
