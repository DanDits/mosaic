package data.image;


/**
 * Created by dd on 03.06.17.
 */
public interface AbstractCanvas {

    void drawBitmap(AbstractBitmap bitmap, int x, int y);

    void drawBitmap(AbstractBitmap bitmap, int x, int y, int fromBitmapX, int fromBitmapY, int toBitmapX, int toBitmapY);

    void clear();

    void drawColor(int color);

    void drawCircle(int centerX, int centerY, int radius, int color);

    void drawQuadraticCurve(int fromX, int fromY, int overX, int overY, int toX, int toY, int color);

    void drawMultiplicativly(AbstractBitmap image, int x, int y);
    void drawMultiplicativly(AbstractBitmap bitmap, int x, int y, int fromBitmapX, int fromBitmapY, int toBitmapX, int toBitmapY);
    void drawBitmapUsingPorterDuff(AbstractBitmap bitmap, int x, int y, PorterDuffMode mode);
    void drawBitmapUsingPorterDuff(AbstractBitmap bitmap, int x, int y, int fromBitmapX, int fromBitmapY,
                                          int toBitmapX, int toBitmapY, PorterDuffMode mode);

    AbstractBitmap obtainImage();

    void drawLine(int fromX, int fromY, int toX, int toY, int color);

    void floodFill(int x, int y, int color);

    void setAntiAliasing(boolean enable);

}
