package util.image;

import data.image.AbstractColor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by dd on 22.06.17.
 */
public class KDColorTree<D extends Colorized> implements Iterable<KDColorTree.Node<D>> {
    private static final int MEDIAN_ESTIMATION_MIN_SAMPLE_SIZE = 10;
    private static final double MEDIAN_ESTIMATION_SAMPLE_FRACTION = 0.01;
    private final ColorSpace space;

    private Node<D> root;
    private int size;

    private KDColorTree(Node<D> root, int size, ColorSpace space) {
        this.root = root;
        this.size = size;
        this.space = space;
    }

    public boolean removeNode(D data) {
        Optional<Node<D>> node = getExistingNode(data);
        if (node.isPresent()) {
            // removing a node from the graph can be costly, if the node is the root then (approximately) half
            // of the nodes have to be visited to look for a replacement
            node.get().remove(space);
            size--;
            if (node.get() == root && size == 0) {
                root = null;
            }
        }
        return node.isPresent();
    }

    private double colorDistance(int color1, int color2) {
        return space.getDistance(color1, color2);
    }

    private double colorComponentDistance(int color1, int color2, int axis) {
        return space.getDistance(color1, color2, axis);
    }

    public Optional<D> getNearestNeighbor(int targetColor) {
        Optional<Node<D>> bestOpt = findNode(root, targetColor, Node::isLeaf);
        Node<D> best;
        double bestDist;
        if (!bestOpt.isPresent()) {
            return Optional.empty();
        }
        best = bestOpt.get();
        if (best == root) {
            return Optional.of(best.data);
        }
        bestDist = colorDistance(targetColor, best.color);

        // start at root again and check weather the circle around best with radius^2=bestDist intersects its area
        int axis;
        Node<D> current;
        double currentDist;
        Stack<Node<D>> nextNodes = new Stack<>();
        Stack<Integer> nextAxis = new Stack<>();
        pushSearchNode(nextNodes, nextAxis, root, 0);
        final int dimension = space.getDimension();
        do {
            current = nextNodes.pop();
            axis = nextAxis.pop();
            currentDist = colorDistance(current.color, targetColor);
            if (currentDist < bestDist) {
                best = current;
                bestDist = currentDist;
            }
            double currentComponentDist = colorComponentDistance(targetColor, current.color, axis);
            boolean isInLeftHalfplane = getValueByAxis(space, targetColor, axis) < getValueByAxis(space, current.color, axis);
            if (bestDist < currentComponentDist) {
                // completely on one side of the tree, other can be forgotten
                if (isInLeftHalfplane) {
                    // its in the left part, the right part can be discarded
                    pushSearchNode(nextNodes, nextAxis, current.leftChild, (axis + 1) % dimension);
                } else {
                    pushSearchNode(nextNodes, nextAxis, current.rightChild, (axis + 1) % dimension);
                }
            } else {
                Node<D> first = current.rightChild, second = current.leftChild;
                if (isInLeftHalfplane) {
                    first = current.leftChild;
                    second = current.rightChild;
                }
                pushSearchNode(nextNodes, nextAxis, second, (axis + 1) % dimension);
                pushSearchNode(nextNodes, nextAxis, first, (axis + 1) % dimension);
            }
        } while (!nextNodes.isEmpty());

        return Optional.of(best.data);
    }

    private static <D extends Colorized> void pushSearchNode(Stack<Node<D>> nextNodes, Stack<Integer> nextAxis,
                                           Node<D> next, int axis) {
        if (next != null) {
            nextNodes.push(next);
            nextAxis.push(axis);
        }
    }

    private Optional<Node<D>> findNode(Node<D> startNode, int targetColor, Predicate<Node<D>> stopCondition) {
        int depth = 0;
        int axis;
        Node<D> current = startNode;
        while (current != null) {
            if (stopCondition.test(current)) {
                return Optional.of(current);
            }
            axis = depth % space.getDimension();
            double currValue = getValueByAxis(space, current.color, axis);
            // go down the best possible path until stopCondition holds or there is no more child node
            if (getValueByAxis(space, targetColor, axis) < currValue) {
                current = current.leftChild != null ? current.leftChild : current.rightChild;
            } else {
                current = current.rightChild != null ? current.rightChild : current.leftChild;
            }
            depth++;
        }
        return Optional.empty();
    }

    private Optional<Node<D>> getExistingNode(D data) {
        if (data == null) {
            return Optional.empty();
        }
        int targetColor = data.getColor();
        return findNode(root, targetColor, current -> current.color == targetColor && (data.equals(current.data)));
    }

    @Override
    public String toString() {
        return "Tree:\n" + root;
    }

    @Override
    public Iterator<Node<D>> iterator() {
        return new NodesIterator<>(root);
    }

    public int size() {
        return size;
    }

    private static class NodesIterator<D extends Colorized> implements Iterator<Node<D>> {

        private final List<Node<D>> pending;

        NodesIterator(Node<D> root) {
            this.pending = new ArrayList<>();
            if (root != null) {
                pending.add(root);
            }
        }

        @Override
        public boolean hasNext() {
            return !pending.isEmpty();
        }

        @Override
        public Node<D> next() {
            if (pending.size() == 0) {
                throw new NoSuchElementException();
            }
            Node<D> current = pending.remove(0);
            if (current.leftChild != null) {
                pending.add(current.leftChild);
            }
            if (current.rightChild != null) {
                pending.add(current.rightChild);
            }
            return current;
        }
    }

    public static class Node<D extends Colorized> {
        private Node<D> parent;
        private int color;
        private Node<D> leftChild;
        private Node<D> rightChild;
        private D data;

        Node(D data, Node<D> leftChild, Node<D> rightChild) {
            this.data = data;
            this.color = data.getColor();
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

        String getColorString() {
            return AbstractColor.alpha(color) + "," + AbstractColor.red(color) + ","
                    + AbstractColor.green(color) + "," + AbstractColor.blue(color);
        }

        @Override
        public String toString() {
            String children = isLeaf() ? "" : ": " + leftChild + "__" + rightChild;
            return "(" + getColorString() + children + ")";
        }

        private void remove(ColorSpace space) {
            int nodeDimension = space.getDimension();
            if (rightChild != null || leftChild != null) {
                int axis = calculateAxis(nodeDimension);
                Node<D> replacement = findReplacement(space, axis);
                swapNodesData(replacement);
                assert replacement != null;
                replacement.remove(space);
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

        private void swapNodesData(Node<D> toSwap) {
            int tempColor = color;
            D tempData = data;
            color = toSwap.color;
            data = toSwap.data;
            toSwap.color = tempColor;
            toSwap.data = tempData;
        }

        private Node<D> findReplacement(ColorSpace space, int axis) {
            if (rightChild != null) {
                Optional<Node<D>> opt = rightChild.makeStream().min(Comparator.comparingDouble(node -> getValueByAxis(space, node.color, axis)));
                if (opt.isPresent()) {
                    return opt.get();
                }
            }
            if (leftChild != null) {
                Optional<Node<D>> opt = leftChild.makeStream().max(Comparator.comparingDouble(node -> getValueByAxis(space, node.color, axis)));
                if (opt.isPresent()) {
                    return opt.get();
                }
            }
            return null;
        }

        Stream<Node<D>> makeStream() {
            Iterator<Node<D>> iterator = new NodesIterator<>(this);
            Iterable<Node<D>> iterable = () -> iterator;
            return StreamSupport.stream(iterable.spliterator(), false);
        }

        private int calculateAxis(int nodeDimension) {
            if (parent == null) {
                return 0;
            }
            return (parent.calculateAxis(nodeDimension) + 1) % nodeDimension;
        }

        public D getData() {
            return data;
        }
    }

    private static double getValueByAxis(ColorSpace space, int color, int axis) {
        return space.getValue(color, axis);
    }

    private static int getSampleSize(int total) {
        // must be (non-strict) monotonously decreasing for smaller parameter
        return Math.max(Math.min(total, MEDIAN_ESTIMATION_MIN_SAMPLE_SIZE),
                        (int) (MEDIAN_ESTIMATION_SAMPLE_FRACTION * total));
    }

    public static<D extends Colorized> KDColorTree<D> make(Random random, Collection<D> data, ColorSpace space) {
        List<D> dataList = new ArrayList<>(data);
        double[] buffer = new double[getSampleSize(data.size())];
        Node<D> root = makeRecursively(random, space.getDimension(), dataList, buffer, 0, space);
        return new KDColorTree<>(root, data.size(), space);
    }

    private static<D extends Colorized> Node<D> makeRecursively(Random rnd, int dimension, List<D> data,
                                              double[] buffer, int depth, ColorSpace space) {
        if (data.size() == 0) {
            return null;
        }
        int axis = depth % dimension;

        D medianData = approximateMedianArgb(space, rnd, data, buffer, axis);
        double median = getValueByAxis(space, medianData.getColor(), axis);
        List<D> leftColors = data.stream().filter(ele -> getValueByAxis(space, ele.getColor(), axis) < median)
                                 .collect(Collectors.toList());
        List<D> rightColors = data.stream().filter(ele -> getValueByAxis(space, ele.getColor(), axis) >= median
                                                                         && ele != medianData)
                                 .collect(Collectors.toList());
        // recursion depth is approximately logarithmic (base 2) as we use the (approximated) median
        return new Node<D>(medianData,
                           makeRecursively(rnd, dimension, leftColors, buffer, depth + 1, space),
                            makeRecursively(rnd, dimension, rightColors, buffer, depth + 1, space));
    }

    private static<D extends Colorized> D approximateMedianArgb(ColorSpace space, Random rnd, List<D> data,
                                             double[] buffer, int axis) {
        int dataSize = data.size();
        int sampleSize = getSampleSize(dataSize);
        for (int i = 0; i < sampleSize; i++) {
            int pos = i + rnd.nextInt(dataSize - i);
            // swap, this prevents reusing an element and allows easy access of sample in first sample array entries
            D temp = data.get(pos);
            data.set(pos, data.get(i));
            data.set(i, temp);
            buffer[i] = getValueByAxis(space, temp.getColor(), axis);
        }
        Arrays.sort(buffer, 0, sampleSize);
        double medianValue = buffer[sampleSize / 2];
        // we want the total argb not only the value we sorted after, but the colors array is sorted differently
        for (int i = 0; i < sampleSize; i++) {
            D current = data.get(i);
            if (getValueByAxis(space, current.getColor(), axis) == medianValue) {
                return current; // get first with equal median value
            }
        }
        throw new IllegalStateException("Did not find the median value again.");
    }
}
