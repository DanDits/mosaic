package util;

import org.junit.Before;
import org.junit.Test;
import util.clustering.CollectionPointCloud;
import util.clustering.KDFlatTree;
import util.clustering.Point;
import util.clustering.PointCloud;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PointCloudTest {

    private PointCloud simpleCloud;
    private PointCloud treeCloud;
    private List<Point> points;

    @Before
    public void initClouds() {
        points = new ArrayList<>();
        addPoint(points, 7, 0);
        addPoint(points, 6, 1);
        addPoint(points, 5, 2);
        addPoint(points, 11, 5);
        addPoint(points, 11, 0);
        simpleCloud = new CollectionPointCloud(points);
        Random rnd = new Random(1337);
        treeCloud = KDFlatTree.make(rnd, points);
    }

    private void addPoint(Collection<Point> points, double x, double y) {
        points.add(new Point(x, y));
    }

    @Test
    public void testAll() {
        int pointsCount = 5;
        assertEquals(pointsCount, points.size());
        assertEquals(pointsCount, simpleCloud.getAll().size());
        assertEquals(pointsCount, treeCloud.getAll().size());
    }

    @Test
    public void size() {
        int pointsCount = 5;
        assertEquals(pointsCount, points.size());
        assertEquals(pointsCount, simpleCloud.size());
        assertEquals(pointsCount, treeCloud.size());
    }

    @Test
    public void shortestDistanceExact() {
        Point expected = new Point(11, 5);
        assertTrue(isPoint(simpleCloud.getClosestPoint(11, 5), expected));
        assertTrue(isPoint(treeCloud.getClosestPoint(11, 5), expected));
    }

    @Test
    public void shortestDistanceSimpleInexact() {
        Point expected = new Point(11, 5);
        assertTrue(isPoint(simpleCloud.getClosestPoint(12, 4), expected));
        assertTrue(isPoint(treeCloud.getClosestPoint(12, 4), expected));
    }

    @Test
    public void shortestDistanceInexact() {
        Point expected = new Point(6, 1);
        assertTrue(isPoint(simpleCloud.getClosestPoint(4, -1), expected));
        assertTrue(isPoint(treeCloud.getClosestPoint(4, -1), expected));
    }

    @Test
    public void pointsInDistanceAll() {
        Point center = new Point(8, 2);
        double dist = Math.sqrt(18.) + 0.001;
        testPointsInRange(points, center.getX(), center.getY(), dist);
    }

    @Test
    public void pointsInDistanceNone() {
        Point center = new Point(8, 2);
        double dist = 1.;
        testPointsInRange(Collections.emptyList(), center.getX(), center.getY(), dist);
    }

    @Test
    public void pointsInDistanceSome() {
        Point center = new Point(6.2, 1);
        double dist = Math.sqrt(2) + 0.001;
        testPointsInRange(Arrays.asList(new Point(6, 1), new Point(7, 0)), center.getX(), center.getY(), dist);
    }

    @Test
    public void testRandomBigCloud() {
        int COUNT = 100000;
        Random rnd = new Random(1337);
        List<Point> randomPoints = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            randomPoints.add(new Point(rnd.nextDouble() * 2 - 1, rnd.nextDouble() * 2 - 1));
        }
        long currentTiming;
        long timeSimpleBuilding = 0;
        long timeTreeBuilding = 0;


        currentTiming = System.currentTimeMillis();
        simpleCloud = new CollectionPointCloud(randomPoints);
        timeSimpleBuilding += System.currentTimeMillis() - currentTiming;
        Random rnd2 = new Random(1337);
        currentTiming = System.currentTimeMillis();
        treeCloud = KDFlatTree.make(rnd2, randomPoints);
        timeTreeBuilding += System.currentTimeMillis() - currentTiming;

        assertEquals(COUNT, simpleCloud.size());
        assertEquals(COUNT, treeCloud.size());

        System.out.println("Timings building for " + COUNT + " points: " + timeSimpleBuilding + " | " + timeTreeBuilding);

        long timeSimpleCP = 0;
        long timeTreeCP = 0;


        int RANDOM_NEARESTS = 10000;
        for (int i = 0; i < RANDOM_NEARESTS; i++) {
            double x = rnd.nextDouble() * 2 - 1;
            double y = rnd.nextDouble() * 2 - 1;
            currentTiming = System.currentTimeMillis();
            Point expected = simpleCloud.getClosestPoint(x, y).get();
            timeSimpleCP += System.currentTimeMillis() - currentTiming;
            currentTiming = System.currentTimeMillis();
            assertTrue(isPoint(treeCloud.getClosestPoint(x, y), expected));
            timeTreeCP += System.currentTimeMillis() - currentTiming;
        }
        System.out.println("Timings nearest for " + COUNT + " points: " + timeSimpleCP + " | " + timeTreeCP);

        long timeSimpleD = 0;
        long timeTreeD = 0;

        int RANDOM_IN_DISTANCE = 10000;
        for (int i = 0; i < RANDOM_IN_DISTANCE; i++) {
            double x = rnd.nextDouble() * 2 - 1;
            double y = rnd.nextDouble() * 2 - 1;
            double dist = rnd.nextDouble() * 0.1;
            currentTiming = System.currentTimeMillis();
            Collection<Point> expected = simpleCloud.getPointsInDistance(x, y, dist);
            timeSimpleD += System.currentTimeMillis() - currentTiming;
            currentTiming = System.currentTimeMillis();
            Collection<Point> result = treeCloud.getPointsInDistance(x, y, dist);
            timeTreeD += System.currentTimeMillis() - currentTiming;
            assertEquals(expected.size(), result.size());
            for (Point point : expected) {
                assertTrue(result.contains(point));
            }
        }
        System.out.println("Timings distance for " + COUNT + " points: " + timeSimpleD + " | " + timeTreeD);
    }

    private void testPointsInRange(List<Point> expectedPoints, double centerX, double centerY, double dist) {
        Collection<Point> result1 = simpleCloud.getPointsInDistance(centerX, centerY, dist);
        Collection<Point> result2 = treeCloud.getPointsInDistance(centerX, centerY, dist);
        assertEquals(expectedPoints.size(), result1.size());
        assertEquals(expectedPoints.size(), result2.size());
        for (Point point : expectedPoints) {
            assertTrue(result1.contains(point));
            assertTrue(result2.contains(point));
        }
    }

    private static boolean isPoint(Optional<Point> pointOpt, Point expected) {
        return pointOpt.isPresent() && pointOpt.get().equals(expected);
    }
}
