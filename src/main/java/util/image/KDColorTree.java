package util.image;

import data.image.AbstractColor;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by dd on 22.06.17.
 */
public class KDColorTree<D> implements Iterable<KDColorTree.Node<D>> {
    private static final int MEDIAN_ESTIMATION_MIN_SAMPLE_SIZE = 10;
    private static final double MEDIAN_ESTIMATION_SAMPLE_FRACTION = 0.01;

    private Node<D> root;
    private int dimension;
    private int size;

    private KDColorTree(Node<D> root, int size, int dimension) {
        this.root = root;
        this.size = size;
        this.dimension = dimension;
    }

    public boolean removeNode(int color, D data) {
        Optional<Node<D>> node = getExistingNode(color, data);
        if (node.isPresent()) {
            // removing a node from the graph can be costly, if the node is the root then (approximately) half
            // of the nodes have to be visited to look for a replacement
            node.get().remove(dimension);
            size--;
            if (node.get() == root && size == 0) {
                root = null;
            }
        }
        return node.isPresent();
    }

    private int colorDistance(int color1, int color2) {
        int sum = 0;
        for (int i = 0; i < dimension; i++) {
            sum += colorComponentDistance(color1, color2, i);
        }
        return sum;
    }

    private int colorComponentDistance(int color1, int color2, int axis) {
        int dist = Math.abs(getValueByAxis(color1, axis) - getValueByAxis(color2, axis));
        return dist * dist;
    }

    public Optional<D> getNearestNeighbor(int targetColor) {
        Optional<Node<D>> bestOpt = findNode(root, targetColor, Node::isLeaf);
        Node<D> best = null;
        int bestDist;
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
        int currentDist;
        Stack<Node<D>> nextNodes = new Stack<>();
        Stack<Integer> nextAxis = new Stack<>();
        appendSearchNode(nextNodes, nextAxis, root, 0);
        do {
            current = nextNodes.pop();
            axis = nextAxis.pop();
            currentDist = colorDistance(current.color, targetColor);
            if (currentDist < bestDist) {
                best = current;
                bestDist = currentDist;
            }
            int currentComponentDist = colorComponentDistance(targetColor, current.color, axis);
            boolean addLeft = true, addRight = true; // by default assume that it overlaps both parts
            if (bestDist < currentComponentDist) {
                // completely on one side of the tree, other can be forgotten
                if (getValueByAxis(targetColor, axis) < getValueByAxis(current.color, axis)) {
                    // its in the left part, the right part can be discarded
                    addRight = false;
                } else {
                    addLeft = false;
                }
            }
            if (addLeft && current.leftChild != null) {
                appendSearchNode(nextNodes, nextAxis, current.leftChild, (axis + 1) % dimension);
            }
            if (addRight && current.rightChild != null) {
                appendSearchNode(nextNodes, nextAxis, current.rightChild, (axis + 1) % dimension);
            }
        } while (!nextNodes.isEmpty());

        return Optional.of(best.data);
    }

    private static <D> void appendSearchNode(Stack<Node<D>> nextNodes, Stack<Integer> nextAxis,
                                             Node<D> next, int axis) {
        nextNodes.push(next);
        nextAxis.push(axis);
    }

    private Optional<Node<D>> findNode(Node<D> startNode, int targetColor, Predicate<Node<D>> stopCondition) {
        int depth = 0;
        int axis;
        Node<D> current = startNode;
        while (current != null) {
            if (stopCondition.test(current)) {
                return Optional.of(current);
            }
            axis = depth % dimension;
            int currValue = getValueByAxis(current.color, axis);
            // go down the best possible path until stopCondition holds or there is no more child node
            if (getValueByAxis(targetColor, axis) < currValue) {
                current = current.leftChild != null ? current.leftChild : current.rightChild;
            } else {
                current = current.rightChild != null ? current.rightChild : current.leftChild;
            }
            depth++;
        }
        return Optional.empty();
    }

    private Optional<Node<D>> getExistingNode(int targetColor, D data) {
        if (data == null) {
            return Optional.empty();
        }
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

    private static class NodesIterator<D> implements Iterator<Node<D>> {

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

    public static class Node<D> {
        private Node<D> parent;
        private int color;
        private Node<D> leftChild;
        private Node<D> rightChild;
        private D data;

        Node(D data, int color, Node<D> leftChild, Node<D> rightChild) {
            this.data = data;
            this.color = color;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            if (leftChild != null) {
                leftChild.parent = this;
            }
            if (rightChild != null) {
                rightChild.parent = this;
            }
        }

        public boolean isLeaf() {
            return leftChild == null && rightChild == null;
        }

        public String getColorString() {
            return AbstractColor.alpha(color) + "," + AbstractColor.red(color) + ","
                    + AbstractColor.green(color) + "," + AbstractColor.blue(color);
        }

        @Override
        public String toString() {
            String children = isLeaf() ? "" : ": " + leftChild + "__" + rightChild;
            return "(" + getColorString() + children + ")";
        }

        private void remove(int nodeDimension) {
            if (rightChild != null || leftChild != null) {
                int axis = calculateAxis(nodeDimension);
                Node<D> replacement = findReplacement(axis);
                swapNodesData(replacement);
                assert replacement != null;
                replacement.remove(nodeDimension);
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

        private Node<D> findReplacement(int axis) {
            if (rightChild != null) {
                Optional<Node<D>> opt = rightChild.makeStream().min(Comparator.comparingInt(node -> getValueByAxis(node.color, axis)));
                if (opt.isPresent()) {
                    return opt.get();
                }
            }
            if (leftChild != null) {
                Optional<Node<D>> opt = leftChild.makeStream().max(Comparator.comparingInt(node -> getValueByAxis(node.color, axis)));
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

    private static int getValueByAxis(int color, int axis) {
        switch (axis) {
            case 0:
                return AbstractColor.red(color);
            case 1:
                return AbstractColor.green(color);
            case 2:
                return AbstractColor.blue(color);
            case 3:
                return AbstractColor.alpha(color);
            default:
                throw new IllegalArgumentException("Illegal axis " + axis);
        }
    }

    private static int getSampleSize(int total) {
        // must be (non-strict) monotonously decreasing for smaller parameter
        return Math.max(Math.min(total, MEDIAN_ESTIMATION_MIN_SAMPLE_SIZE),
                        (int) (MEDIAN_ESTIMATION_SAMPLE_FRACTION * total));
    }

    public static<D> KDColorTree<D> make(Random random, Collection<D> data, Function<D, Integer> colorExtractor, boolean useAlpha) {
        int dimension = useAlpha ? 4 : 3;
        List<D> dataList = new ArrayList<>(data);
        int[] buffer = new int[getSampleSize(data.size())];
        Node<D> root = makeRecursively(random, dimension, dataList, colorExtractor, buffer, 0);
        return new KDColorTree<>(root, data.size(), dimension);
    }

    private static<D> Node<D> makeRecursively(Random rnd, int dimension, List<D> data,
                                              Function<D, Integer> colorExtractor, int[] buffer, int depth) {
        if (data.size() == 0) {
            return null;
        }
        int axis = depth % dimension;

        D medianData = approximateMedianArgb(rnd, data, colorExtractor, buffer, axis);
        int median = getValueByAxis(colorExtractor.apply(medianData), axis);
        List<D> leftColors = data.stream().filter(ele -> getValueByAxis(colorExtractor.apply(ele), axis) < median)
                                 .collect(Collectors.toList());
        List<D> rightColors = data.stream().filter(ele -> getValueByAxis(colorExtractor.apply(ele), axis) >= median
                                                                         && ele != medianData)
                                 .collect(Collectors.toList());
        // recursion depth is approximately logarithmic (base 2) as we use the (approximated) median
        return new Node<D>(medianData, colorExtractor.apply(medianData),
                           makeRecursively(rnd, dimension, leftColors, colorExtractor, buffer, depth + 1),
                            makeRecursively(rnd, dimension, rightColors, colorExtractor, buffer, depth + 1));
    }

    private static<D> D approximateMedianArgb(Random rnd, List<D> data, Function<D, Integer> colorExtractor,
                                             int[] buffer, int axis) {
        int dataSize = data.size();
        int sampleSize = getSampleSize(dataSize);
        for (int i = 0; i < sampleSize; i++) {
            int pos = i + rnd.nextInt(dataSize - i);
            // swap, this prevents reusing an element and allows easy access of sample in first sample array entries
            D temp = data.get(pos);
            data.set(pos, data.get(i));
            data.set(i, temp);
            buffer[i] = getValueByAxis(colorExtractor.apply(temp), axis);
        }
        Arrays.sort(buffer, 0, sampleSize);
        int medianValue = buffer[sampleSize / 2];
        // we want the total argb not only the value we sorted after, but the colors array is sorted differently
        for (int i = 0; i < sampleSize; i++) {
            D current = data.get(i);
            if (getValueByAxis(colorExtractor.apply(current), axis) == medianValue) {
                return current; // get first with equal median value
            }
        }
        throw new IllegalStateException("Did not find the median value again.");
    }
}
