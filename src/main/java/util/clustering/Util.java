package util.clustering;

import util.image.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Util {

    public static BufferedImage loadImage(String path) {
        BufferedImage source;
        try {
            source = ImageIO.read(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return source;
    }

    public static List<Point> loadPointsFromBitmap(BufferedImage source) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < source.getWidth(); i++) {
            for (int j = 0; j < source.getHeight(); j++) {
                if (Color.red(source.getRGB(i, j)) < 0xA0) {
                    points.add(new Point(i, j));
                }
            }
        }
        return points;
    }

}
