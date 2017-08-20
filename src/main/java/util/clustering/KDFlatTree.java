package util.clustering;

import util.image.Colorized;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class KDFlatTree implements Iterable<Point>, PointCloud {
    private static final int DIMENSION = 2;
    private static final int MEDIAN_ESTIMATION_MIN_SAMPLE_SIZE = 10;
    private static final double MEDIAN_ESTIMATION_SAMPLE_FRACTION = 0.01;

    private Node root;
    private int size;

    private KDFlatTree(Node root, int size) {
        this.root = root;
        this.size = size;
    }

    public boolean removeNode(Point data) {
        Optional<Node> node = getExistingNode(data);
        if (node.isPresent()) {
            // removing a node from the graph can be costly, if the node is the root then (approximately) half
            // of the nodes have to be visited to look for a replacement
            node.get().remove();
            size--;
            if (node.get() == root && size == 0) {
                root = null;
            }
        }
        return node.isPresent();
    }

    private double pointComparableDistance(Point point1, Point point2) {
        return point1.getComparableDistance(point2.getX(), point2.getY());
    }

    private double pointComparableComponentDistance(Point point1, Point point2, int axis) {
        return point1.getComparableDistance(point2.getX(), point2.getY(), axis == 0);
    }

    public Optional<Point> getNearestNeighbor(Point targetPoint) {
        Optional<Node> bestOpt = findNode(root, targetPoint, Node::isLeaf);
        Node best;
        double bestDist;
        if (!bestOpt.isPresent()) {
            return Optional.empty();
        }
        best = bestOpt.get();
        if (best == root) {
            return Optional.of(best.data);
        }
        bestDist = pointComparableDistance(targetPoint, best.getData());

        // start at root again and check weather the circle around best with radius=bestDist intersects its area
        int axis;
        Node current;
        double currentDist;
        Stack<Node> nextNodes = new Stack<>(); // use stacks for depth first search, faster than breath first
        Stack<Integer> nextAxis = new Stack<>();
        pushSearchNode(nextNodes, nextAxis, root, 0);
        do {
            current = nextNodes.pop();
            axis = nextAxis.pop();
            currentDist = pointComparableDistance(current.getData(), targetPoint);
            if (currentDist < bestDist) {
                best = current;
                bestDist = currentDist;
            }
            double currentComponentDist = pointComparableComponentDistance(targetPoint, current.getData(), axis);
            boolean isInLeftHalfplane = getValueByAxis(targetPoint, axis) < getValueByAxis(current.getData(), axis);
            if (bestDist < currentComponentDist) {
                // completely on one side of the tree, other can be forgotten
                if (isInLeftHalfplane) {
                    pushSearchNode(nextNodes, nextAxis, current.leftChild, (axis + 1) % DIMENSION);
                } else {
                    pushSearchNode(nextNodes, nextAxis, current.rightChild, (axis + 1) % DIMENSION);
                }
            } else {
                // circle intersects both sides.
                // optimization to search the half plane first that contains the current node
                // if there are many nodes that are well spread this makes it more likely to find a good one in this
                // part
                Node first = current.rightChild, second = current.leftChild;
                if (isInLeftHalfplane) {
                    first = current.leftChild;
                    second = current.rightChild;
                }
                pushSearchNode(nextNodes, nextAxis, second, (axis + 1) % DIMENSION);
                pushSearchNode(nextNodes, nextAxis, first, (axis + 1) % DIMENSION);
            }
        } while (!nextNodes.isEmpty());

        return Optional.of(best.data);
    }

    private List<Point> getPointsInDistance(Point center, double comparableDistance) {
        List<Point> found = new ArrayList<>();

        int axis;
        Node current;
        double currentDist;
        Stack<Node> nextNodes = new Stack<>(); // use stacks for depth first search, faster than breath first
        Stack<Integer> nextAxis = new Stack<>();
        pushSearchNode(nextNodes, nextAxis, root, 0);
        do {
            current = nextNodes.pop();
            axis = nextAxis.pop();
            currentDist = pointComparableDistance(current.getData(), center);
            if (currentDist <= comparableDistance) {
                found.add(current.getData());
            }
            double currentComponentDist = pointComparableComponentDistance(center, current.getData(), axis);
            boolean isInLeftHalfplane = getValueByAxis(center, axis) < getValueByAxis(current.getData(), axis);
            if (comparableDistance < currentComponentDist) {
                // completely on one side of the tree, other can be forgotten
                if (isInLeftHalfplane) {
                    pushSearchNode(nextNodes, nextAxis, current.leftChild, (axis + 1) % DIMENSION);
                } else {
                    pushSearchNode(nextNodes, nextAxis, current.rightChild, (axis + 1) % DIMENSION);
                }
            } else {
                Node first = current.leftChild;
                Node second = current.rightChild;
                pushSearchNode(nextNodes, nextAxis, second, (axis + 1) % DIMENSION);
                pushSearchNode(nextNodes, nextAxis, first, (axis + 1) % DIMENSION);
            }
        } while (!nextNodes.isEmpty());

        return found;
    }

    private static <D extends Colorized> void pushSearchNode(Stack<Node> nextNodes, Stack<Integer> nextAxis,
                                           Node next, int axis) {
        if (next != null) {
            nextNodes.push(next);
            nextAxis.push(axis);
        }
    }

    private Optional<Node> findNode(Node startNode, Point target, Predicate<Node> stopCondition) {
        int depth = 0;
        int axis;
        Node current = startNode;
        while (current != null) {
            if (stopCondition.test(current)) {
                return Optional.of(current);
            }
            axis = depth % DIMENSION;
            double currValue = getValueByAxis(current.getData(), axis);
            // go down the best possible path until stopCondition holds or there is no more child node
            if (getValueByAxis(target, axis) < currValue) {
                current = current.leftChild != null ? current.leftChild : current.rightChild;
            } else {
                current = current.rightChild != null ? current.rightChild : current.leftChild;
            }
            depth++;
        }
        return Optional.empty();
    }

    private Optional<Node> getExistingNode(Point data) {
        if (data == null) {
            return Optional.empty();
        }
        return findNode(root, data, current -> data.equals(current.getData()));
    }

    @Override
    public String toString() {
        return "Tree:\n" + root;
    }

    @Override
    public Iterator<Point> iterator() {
        return new Iterator<Point>() {
            NodesIterator it = new NodesIterator(root);
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Point next() {
                return it.next().getData();
            }
        };
    }

    /**
     * Returns the tree's size.
     * @return Tree size.
     */
    @Override
    public int size() {
        return size;
    }

    @Override
    public Collection<Point> getAll() {
        List<Point> list = new ArrayList<>();
        iterator().forEachRemaining(list::add);
        return list;
    }

    @Override
    public List<Point> getPointsInDistance(double x, double y, double distance) {
        return getPointsInDistance(new Point(x, y), distance * distance);
    }

    @Override
    public Optional<Point> getClosestPoint(double x, double y) {
        return getNearestNeighbor(new Point(x, y));
    }

    // iterates through the total tree starting at the root
    private static class NodesIterator implements Iterator<Node> {

        private final List<Node> pending;

        NodesIterator(Node root) {
            this.pending = new LinkedList<>();
            if (root != null) {
                pending.add(root);
            }
        }

        @Override
        public boolean hasNext() {
            return !pending.isEmpty();
        }

        @Override
        public Node next() {
            if (pending.size() == 0) {
                throw new NoSuchElementException();
            }
            Node current = pending.remove(0);
            if (current.leftChild != null) {
                pending.add(current.leftChild);
            }
            if (current.rightChild != null) {
                pending.add(current.rightChild);
            }
            return current;
        }
    }

    public static class Node {
        private Node parent;
        private Node leftChild;
        private Node rightChild;
        private Point data;

        Node(Point data, Node leftChild, Node rightChild) {
            this.data = data;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            if (leftChild != null) {
                leftChild.parent = this;
            }
            if (rightChild != null) {
                rightChild.parent = this;
            }
        }

        boolean isLeaf() {
            return leftChild == null && rightChild == null;
        }

        private void remove() {
            if (rightChild != null || leftChild != null) {
                int axis = calculateAxis(DIMENSION);
                Node replacement = findReplacement(axis);
                swapNodesData(replacement);
                replacement.remove();
            } else if (parent != null) {
                // is a leaf node (and not the naked root)
                if (parent.leftChild == this) {
                    parent.leftChild = null;
                } else if (parent.rightChild == this) {
                    parent.rightChild = null;
                } else {
                    throw new IllegalStateException("Abandoned node, parent doesn't accept it!");
                }
                parent = null;
            }
        }

        private void swapNodesData(Node toSwap) {
            Point tempData = data;
            data = toSwap.data;
            toSwap.data = tempData;
        }

        private Node findReplacement(int axis) {
            if (rightChild != null) {
                Optional<Node> opt = rightChild.makeStream().min(Comparator.comparingDouble(node -> getValueByAxis(node.data, axis)));
                if (opt.isPresent()) {
                    return opt.get();
                }
            }
            if (leftChild != null) {
                Optional<Node> opt = leftChild.makeStream().max(Comparator.comparingDouble(node -> getValueByAxis(node.data, axis)));
                if (opt.isPresent()) {
                    return opt.get();
                }
            }
            throw new IllegalStateException("Did not find replacement for node "+ this + " on axis "+ axis);
        }

        Stream<Node> makeStream() {
            Iterator<Node> iterator = new NodesIterator(this);
            Iterable<Node> iterable = () -> iterator;
            return StreamSupport.stream(iterable.spliterator(), false);
        }

        private int calculateAxis(int nodeDimension) {
            if (parent == null) {
                return 0;
            }
            return (parent.calculateAxis(nodeDimension) + 1) % nodeDimension;
        }

        public Point getData() {
            return data;
        }
    }

    private static double getValueByAxis(Point data, int axis) {
        if (axis == 0) {
            return data.getX();
        }
        return data.getY();
    }

    private static int getSampleSize(int total) {
        // must be (non-strict) monotonously decreasing for smaller parameter
        return Math.max(Math.min(total, MEDIAN_ESTIMATION_MIN_SAMPLE_SIZE),
                        (int) (MEDIAN_ESTIMATION_SAMPLE_FRACTION * total));
    }

    public static KDFlatTree make(Random random, Collection<Point> data) {
        List<Point> dataList = new ArrayList<>(data);
        double[] buffer = new double[getSampleSize(data.size())];
        Node root = makeRecursively(random, dataList, buffer, 0);
        return new KDFlatTree(root, data.size());
    }

    private static Node makeRecursively(Random rnd, List<Point> data,
                                              double[] buffer, int depth) {
        if (data.size() == 0) {
            return null;
        }
        int axis = depth % DIMENSION;

        Point medianData = approximateMedianArgb(rnd, data, buffer, axis);
        double median = getValueByAxis(medianData, axis);
        List<Point> leftColors = data.stream().filter(ele -> getValueByAxis(ele, axis) < median)
                                 .collect(Collectors.toList());
        List<Point> rightColors = data.stream().filter(ele -> getValueByAxis(ele, axis) >= median
                                                                         && ele != medianData)
                                 .collect(Collectors.toList());
        // recursion depth is approximately logarithmic (base 2) as we use the (approximated) median
        return new Node(medianData,
                           makeRecursively(rnd, leftColors, buffer, depth + 1),
                            makeRecursively(rnd, rightColors, buffer, depth + 1));
    }

    private static Point approximateMedianArgb(Random rnd, List<Point> data,
                                             double[] buffer, int axis) {
        int dataSize = data.size();
        int sampleSize = getSampleSize(dataSize);
        for (int i = 0; i < sampleSize; i++) {
            int pos = i + rnd.nextInt(dataSize - i);
            // swap, this prevents reusing an element and allows easy access of sample in first sample array entries
            Point temp = data.get(pos);
            data.set(pos, data.get(i));
            data.set(i, temp);
            buffer[i] = getValueByAxis(temp, axis);
        }
        Arrays.sort(buffer, 0, sampleSize);
        double medianValue = buffer[sampleSize / 2];
        // we want the total argb not only the value we sorted after, but the colors array is sorted differently
        for (int i = 0; i < sampleSize; i++) {
            Point current = data.get(i);
            if (getValueByAxis(current, axis) == medianValue) {
                return current; // get first with equal median value
            }
        }
        throw new IllegalStateException("Did not find the median value again.");
    }
}
