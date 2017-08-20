package util.clustering;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PointCloud extends Iterable<Point> {
    Collection<Point> getAll();
    List<Point> getPointsInDistance(double x, double y, double range);
    Optional<Point> getClosestPoint(double x, double y);

    int size();
}
