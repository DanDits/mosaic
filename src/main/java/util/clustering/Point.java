package util.clustering;

public class Point {
    private final double x;
    private final double y;
    private PointMark marker = PointMark.NONE;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean isWithinDistance(double x, double y, double distance) {
        return getComparableDistance(x, y) <= distance * distance;
    }

    public double getComparableDistance(double x, double y) {
        return getComparableDistance(x, y, true) + getComparableDistance(x, y,false);
    }

    public double getComparableDistance(double x, double y, boolean firstAxis) {
        if (firstAxis) {
            final double dx = (x - this.x);
            return dx * dx;
        }
        final double dy = (y - this.y);
        return dy * dy;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (Double.compare(point.x, x) != 0) return false;
        return Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public void setMarker(PointMark marker) {
        this.marker = marker;
    }

    public PointMark getMarker() {
        return marker;
    }
}
