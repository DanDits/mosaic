package util.clustering;

import java.util.*;
import java.util.stream.Collectors;

public class KMeansClusterer {

    private static final int MAX_RECALCULATIONS_BASE = 2;
    private static final int MAX_RECALCULATIONS_LINEAR_GROWTH = 1;
    private final Random rnd;
    private final PointCloud cloud;
    private Point[] clusterCenters;
    private List<Point> all;

    public KMeansClusterer(Random rnd, PointCloud cloud) {
        this.rnd = rnd;
        this.cloud = cloud;
    }

    public void execute(int clusterCount) {
        init(clusterCount);
        run(clusterCount);
    }

    private void init(final int clusterCount) {
        if (clusterCount < 1) {
            throw new IllegalArgumentException("No clusters?!" + clusterCount);
        }
        if (all == null || clusterCenters == null) {
            all = new ArrayList<>(cloud.getAll());  // we want random access
            clusterCenters = new Point[clusterCount];
            initCenters(clusterCount, Collections.emptyList());
        } else {
            List<Point> oldCenters = Arrays.stream(clusterCenters)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            clusterCenters = new Point[clusterCount];
            initCenters(clusterCount, oldCenters);
        }

    }

    private void initCenters(int clusterCount, List<Point> reuseCenters) {

        /* //init (with random centers, also other possibilities as convergence greatly depends on starting values)
        for (int i = 0; i < clusterCenters.length; i++) {
            clusterCenters[i] = all.get(rnd.nextInt(all.size()));
        }*/
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Point p : all) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        final double maxDist = Math.max(1E-10, (maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY));

        int reuseCount = Math.min(clusterCount, reuseCenters.size());
        for (int i = 0; i < reuseCount; i++) {
            clusterCenters[i] = reuseCenters.get(i);
        }

        // init k-means++ style by preferring centers that are further away from the last center
        int allCount = all.size();
        int startIndex = reuseCount;
        if (reuseCount == 0) {
            this.clusterCenters[0] = all.get(rnd.nextInt(allCount));
            startIndex = 1;
        }
        for (int cluster = startIndex; cluster < clusterCount; cluster++) {
            this.clusterCenters[cluster] = all.get(rnd.nextInt(allCount));
            for (int i = 0; i < allCount; i++) {
                Point current = all.get(i);
                double distFraction = this.clusterCenters[cluster - 1].getComparableDistance(current);
                distFraction /= maxDist;
                distFraction *= distFraction;
                distFraction *= distFraction;
                distFraction *= i / (double) allCount;
                if (rnd.nextDouble() < distFraction) {
                    this.clusterCenters[cluster] = current;
                    break;
                }
            }
        }
    }

    private void run(int clusterCount) {
        // k-means algorithm
        final int allCount = all.size();
        final int[] pointClusterIndex = new int[allCount];
        final double[] valuesX = new double[clusterCount];
        final double[] valuesY = new double[clusterCount];
        final int[] clusterSize = new int[clusterCount];
        int redistributionCount = 0;
        final int maxRedistributions = MAX_RECALCULATIONS_BASE + MAX_RECALCULATIONS_LINEAR_GROWTH * clusterCount;
        int changed = Integer.MAX_VALUE;
        int lastChanged;
        do {
            lastChanged = changed;
            changed = 0;
            // redistribute into clusters
            for (int i = 0; i < allCount; i++) {
                Point current = all.get(i);
                double minWeightIncrease = Double.MAX_VALUE;
                int minWeightIncreaseIndex = 0;
                for (int cluster = 0; cluster < clusterCount; cluster++) {
                    double currWeightIncrease = clusterCenters[cluster].getComparableDistance(current);
                    if (currWeightIncrease < minWeightIncrease) {
                        minWeightIncrease = currWeightIncrease;
                        minWeightIncreaseIndex = cluster;
                    }
                }
                int oldNumber = pointClusterIndex[i];
                pointClusterIndex[i] = minWeightIncreaseIndex;
                if (oldNumber != minWeightIncreaseIndex) {
                    changed++;
                }
            }

            // recalculate centers
            Arrays.fill(valuesX, 0);
            Arrays.fill(valuesY, 0);
            Arrays.fill(clusterSize, 0);
            for (int i = 0; i < allCount; i++) {
                int cluster = pointClusterIndex[i];
                Point current = all.get(i);
                valuesX[cluster] += current.getX();
                valuesY[cluster] += current.getY();
                clusterSize[cluster]++;
            }
            for (int cluster = 0; cluster < clusterCount; cluster++) {
                int size = clusterSize[cluster];
                clusterCenters[cluster] = null;
                if (size > 0) {
                    clusterCenters[cluster] = new Point(valuesX[cluster] / size, valuesY[cluster] / size);
                }
            }
            redistributionCount++;
        } while (changed > 0 && changed <= lastChanged && redistributionCount < maxRedistributions);
    }

    public List<Point> getCenters() {
        return Arrays.stream(clusterCenters)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
