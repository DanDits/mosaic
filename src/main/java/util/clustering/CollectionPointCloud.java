package util.clustering;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionPointCloud implements PointCloud {

    private final Collection<Point> points;

    public CollectionPointCloud(Collection<Point> points) {
        this.points = points;
    }
    @Override
    public Collection<Point> getAll() {
        return points;
    }

    @Override
    public List<Point> getPointsInDistance(double x, double y, double distance) {
        return points.stream()
                .filter(point -> point.isWithinDistance(x, y, distance))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Point> getClosestPoint(double x, double y) {
        return points.stream()
                .min(Comparator.comparingDouble(point -> point.getComparableDistance(x, y)));
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public Iterator<Point> iterator() {
        return points.iterator();
    }
}
