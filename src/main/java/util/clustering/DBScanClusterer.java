package util.clustering;

import util.image.Color;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DBScanClusterer {
    private final ArrayList<Cluster> clusters;
    private int minClusterSize;
    private double eps;
    private Set<Point> noise;

    public DBScanClusterer(int minClusterSize, double eps) {
        this.minClusterSize = minClusterSize;
        this.eps = eps;
        noise = new HashSet<>();
        clusters = new ArrayList<>();
    }

    public int execute(PointCloud cloud) {
        clusters.clear();
        noise.clear();
        markPoints(cloud, PointMark.NONE);
        createClusters(cloud);
        return clusters.size();
    }

    public List<List<Point>> getAnalyzedClusters() {
        return clusters.stream()
                       .map(cluster -> cluster.points)
                       .collect(Collectors.toList());
    }

    private void createClusters(PointCloud cloud) {
        for (Point point : cloud) {
            if (point.getMarker() == PointMark.NONE) {
                point.setMarker(PointMark.VISITED);
                List<Point> neighbors = cloud.getPointsInDistance(point.getX(), point.getY(), eps);
                if (neighbors.size() < minClusterSize) {
                    point.setMarker(PointMark.NOISE);
                    noise.add(point);
                } else {
                    Cluster cluster = new Cluster();
                    clusters.add(cluster);
                    expandCluster(cloud, point, neighbors, cluster);
                }
            }
        }
    }

    private void expandCluster(PointCloud cloud, Point point, List<Point> neighbors, Cluster cluster) {
        neighbors = new LinkedList<>(neighbors);
        cluster.addPoint(point); // point was previously not visited
        while (neighbors.size() > 0) {
            // should be constant time operation
            Point neighbor = neighbors.remove(0); // breath first search
            PointMark neighborPrevMark = neighbor.getMarker();
            if (neighborPrevMark == PointMark.NONE) {
                neighbor.setMarker(PointMark.VISITED);
                List<Point> neighborsNeighbors = cloud.getPointsInDistance(neighbor.getX(), neighbor.getY(), eps);
                if (neighborsNeighbors.size() >= minClusterSize) {
                    neighbors.addAll(neighborsNeighbors);
                }
            }
            if (neighborPrevMark != PointMark.VISITED) {
                // was not visited or noise, add to this cluster now
                // if it was visited (by some other cluster) we COULD join clusters here,
                // but we choose to ignore it and create separate clusters (should be rare for most data sets)
                if (neighborPrevMark == PointMark.NOISE) {
                    neighbor.setMarker(PointMark.VISITED); // maybe it used to be noise
                    noise.remove(neighbor);
                }
                cluster.addPoint(neighbor);
            }
        }
    }

    public Collection<Point> getAnalyzedNoise() {
        return noise;
    }

    private void markPoints(PointCloud cloud, PointMark marker) {
        for (Point point : cloud) {
            point.setMarker(marker);
        }
    }

    private static class Cluster {
        private List<Point> points = new ArrayList<>();

        void addPoint(Point other) {
            points.add(other);
        }
    }

    public static void main(String[] args) {
        String in = "/home/daniel/IdeaProjects/mosaic/src/main/resources/util/clustering/wikisrc.png";
        String out = "/home/daniel/IdeaProjects/mosaic/src/main/resources/util/clustering/wikisrc_result.png";

        BufferedImage source = Util.loadImage(in);
        if (source == null) {
            return;
        }
        List<Point> points = Util.loadPointsFromBitmap(source);
        System.out.println("Read " + points.size() + " points from source image.");
        PointCloud cloud = KDFlatTree.make(new Random(1337), points);
        DBScanClusterer clusterer = new DBScanClusterer(10, 15);
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D resultGraphics = result.createGraphics();
        List<Integer> colors = new LinkedList<>();
        colors.add(Color.rgb(255, 0, 0));
        colors.add(Color.rgb(0, 255, 0));
        colors.add(Color.rgb(0, 0, 255));
        Random rnd = new Random(1337);
        int clusterCount = clusterer.execute(cloud);
        System.out.println("Found " + clusterCount + " clusters");
        List<List<Point>> clusters = clusterer.getAnalyzedClusters();
        for (List<Point> cluster : clusters) {
            Integer color;
            if (colors.size() > 0) {
                color = colors.remove(0);
            } else {
                color = Color.rgb(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255));
            }
            for (Point p : cluster) {
                result.setRGB((int) p.getX(), (int) p.getY(), color);
            }
            KMeansClusterer centroidClusterer = doKMeans(cluster);
            for (Point center : centroidClusterer.getValidCenters()) {
                resultGraphics.setColor(java.awt.Color.BLACK);
                int radius = 10;
                resultGraphics.fillOval((int) center.getX() - radius, (int) center.getY() - radius,
                        2 * radius, 2 * radius);
            }
        }
        for (Point p : clusterer.getAnalyzedNoise()) {
            result.setRGB((int) p.getX(), (int) p.getY(), 0xFF000000);
        }
        try {
            ImageIO.write(result, "png", new File(out));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static KMeansClusterer doKMeans(List<Point> cluster) {
        PointCloud cloud = new CollectionPointCloud(cluster);
        int clusterCount = 3;
        Random rnd = new Random(1337);
        KMeansClusterer clusterer = new KMeansClusterer(rnd, cloud);
        clusterer.execute(clusterCount);
        return clusterer;
    }
}
