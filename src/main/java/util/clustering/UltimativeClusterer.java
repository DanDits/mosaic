package util.clustering;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class UltimativeClusterer {

    public static final int DENSITY_MIN_CLUSTER_SIZE = 20; // min cluster size in density clustering step, should be greater than 2 (=dimension): 1 is meaningless
    public static final double DENSITY_EPS = 15; // distance for neighbor search in density clustering step

    private static final double NOISE_POINTS_DRAW_RADIUS = 2; // this space is reserved so that no centroid draws over noise (though noise might overlap)

    /**
     * Maximum draw radius of a centroid, useful to determine if there might be overlap with a noise point.
     * If DENSITY_EPS > 2 * MAXIMUM_CENTROID_DRAW_RADIUS + REQUIRED_CENTROID_GAP_DISTANCE this
     * should happen VERY rarely if the distribution is strange.
     */
    private static final double MAXIMUM_CENTROID_DRAW_RADIUS = 10;

    private static final int MIN_POINTS_PER_CENTROID = 20;

    /**
     * How much distance there has to be between two centroids borders so that their circles do not touch
     * or get too close.
     */
    private static final double REQUIRED_CENTROID_GAP_DISTANCE = 5;

    private final double MIN_REQUIRED_CENTROID_DISTANCE_TO_ITS_CONTENT = 30;
    private final double MAX_ALLOWED_CENTROID_DATA_RADIUS = 50;

    private PointCloud noiseCloud;
    private List<KMeansClusterer> validClusterers = new ArrayList<>();
    private Collection<Point> noisePoints;

    public UltimativeClusterer() {

    }

    public void execute(List<Point> points) {
        PointCloud cloud = KDFlatTree.make(getFreshRandom(), points);
        DBScanClusterer densityClusterer = new DBScanClusterer(DENSITY_MIN_CLUSTER_SIZE, DENSITY_EPS);
        noisePoints = densityClusterer.getAnalyzedNoise();
        int densityClusterCount = densityClusterer.execute(cloud);
        System.out.println("Found " + densityClusterCount + " density clusters.");
        List<List<Point>> densityClusters = densityClusterer.getAnalyzedClusters();
        noiseCloud = KDFlatTree.make(getFreshRandom(), densityClusterer.getAnalyzedNoise());
        List<KMeansClusterer> meansClusterers = densityClusters.stream()
                .map(this::makeMeansClusterer)
                .collect(Collectors.toList());
        runMeanClusterers(meansClusterers);
    }

    private void runMeanClusterers(List<KMeansClusterer> clusterers) {
        List<KMeansClusterer> nextClusterers = new ArrayList<>(clusterers.size());
        System.out.println("Running " + clusterers.size() + " clusters");
        for (KMeansClusterer clusterer : clusterers) {
            int lastCentroidCount = clusterer.getParameterClusterCount();
            int nextCentroidCount = lastCentroidCount * 2;
            if (nextCentroidCount <= 0) {
                nextCentroidCount = 1;
            } else {
                if (nextCentroidCount > clusterer.getPointsCount() / MIN_POINTS_PER_CENTROID) {
                    continue; // too much is too much
                }
            }
            clusterer.execute(nextCentroidCount);
            nextClusterers.add(clusterer);
        }
        validateMeanClusterers(nextClusterers);
    }

    private void validateMeanClusterers(List<KMeansClusterer> clusterers) {
        List<KMeansClusterer> nextClusterers = new ArrayList<>(clusterers.size());
        for (KMeansClusterer clusterer : clusterers) {
            // for all centroids test:
            //  -T1:center close to a point of its cluster? (maybe not so important, might look strange for rings?)
            //  -T2:max draw radius does not intersect with other cluster centroid (and mind the gap)
            //  -T3:max draw radius does not intersect with a noise point, rare if density eps is big enough, but might happen
            //  -T4:if max distance of a point to centroid is too big (TM) (might be wanted so that long stretched density clusters get further split up)
            // if clusterer passes all tests remove it from clusterers
            // all others need to be run again (set a limit: size/K needs to be bigger than some MIN_CLUSTER_SIZE)

            boolean passTests = true;

            List<Point> centers = clusterer.getValidCenters();
            List<Double> minDistances = clusterer.getMinComparableDistancesToValidCenters();
            List<Double> maxDistances = clusterer.getMaxComparableDistancesToValidCenters();
            for (int i = 0; i < centers.size() && passTests; i++) {
                if (minDistances.get(i) > MIN_REQUIRED_CENTROID_DISTANCE_TO_ITS_CONTENT * MIN_REQUIRED_CENTROID_DISTANCE_TO_ITS_CONTENT) {
                    System.out.println("Fail Test 1");
                    passTests = false; // T1 fail
                }
                if (passTests && maxDistances.get(i) > MAX_ALLOWED_CENTROID_DATA_RADIUS * MAX_ALLOWED_CENTROID_DATA_RADIUS) {
                    System.out.println("Fail Test 4, max dist =" + maxDistances.get(i) + " and allowed=" + MAX_ALLOWED_CENTROID_DATA_RADIUS * MAX_ALLOWED_CENTROID_DATA_RADIUS);
                    passTests = false; // T4 fail
                }
                if (passTests && noiseCloud.getPointsInDistance(centers.get(i).getX(), centers.get(i).getY(), NOISE_POINTS_DRAW_RADIUS)
                        .size() > 0) {
                    System.out.println("Fail Test 3");
                    passTests = false; // T3 fail
                }
            }
            if (!passTests) {
                nextClusterers.add(clusterer);
            } else {
                validClusterers.add(clusterer);
            }
        }
        if (nextClusterers.size() > 0) {
            runMeanClusterers(nextClusterers);
        }
    }

    private KMeansClusterer makeMeansClusterer(List<Point> points) {
        PointCloud cloud = new CollectionPointCloud(points);
        return new KMeansClusterer(getFreshRandom(), cloud);
    }

    private Random getFreshRandom() {
        return new Random(1337); // fix random to have reproducable results
    }

    public static void main(String[] args) {
        String in = "/home/daniel/IdeaProjects/mosaic/src/main/resources/util/clustering/wikisrc.png";
        String out = "/home/daniel/IdeaProjects/mosaic/src/main/resources/util/clustering/wikisrc_result_ultimate.png";

        BufferedImage source = Util.loadImage(in);
        if (source == null) {
            return;
        }
        List<Point> points = Util.loadPointsFromBitmap(source);
        UltimativeClusterer clusterer = new UltimativeClusterer();
        clusterer.execute(points);

        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D resultGraphics = result.createGraphics();
        resultGraphics.setColor(Color.WHITE);
        resultGraphics.fillRect(0, 0, result.getWidth(), result.getHeight());
        resultGraphics.setColor(java.awt.Color.BLACK);
        for (KMeansClusterer cl : clusterer.getValids()) {
            for (Point center : cl.getValidCenters()) {
                resultGraphics.fillOval((int) (center.getX() - MAXIMUM_CENTROID_DRAW_RADIUS),
                        (int) (center.getY() - MAXIMUM_CENTROID_DRAW_RADIUS),
                        (int) MAXIMUM_CENTROID_DRAW_RADIUS * 2, (int) MAXIMUM_CENTROID_DRAW_RADIUS * 2);
            }

        }
        resultGraphics.setColor(Color.RED);
        for (Point noise : clusterer.getNoisePoints()) {
            resultGraphics.fillOval((int) (noise.getX() - NOISE_POINTS_DRAW_RADIUS),
                    (int) (noise.getY() - NOISE_POINTS_DRAW_RADIUS),
                    (int) (2 * NOISE_POINTS_DRAW_RADIUS), (int) (2 * NOISE_POINTS_DRAW_RADIUS));
        }
        try {
            ImageIO.write(result, "png", new File(out));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<KMeansClusterer> getValids() {
        return validClusterers;
    }

    public Collection<Point> getNoisePoints() {
        return noisePoints;
    }
}
