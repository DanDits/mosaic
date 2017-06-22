package util.image;

import data.image.AbstractColor;

import java.util.*;
import java.util.function.Function;
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

    private KDColorTree(Node<D> root) {
        this.root = root;
    }

    @Override
    public String toString() {
        return "Tree:\n" + root.toString();
    }

    @Override
    public Iterator<Node<D>> iterator() {
        return new NodesIterator<>(root);
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

        @Override
        public String toString() {
            String children = leftChild == null && rightChild == null ? "" : ": " + leftChild + "__" + rightChild;
            return "(" + AbstractColor.alpha(color) + "," + AbstractColor.red(color) + ","
                    + AbstractColor.green(color) + "," + AbstractColor.blue(color) + children + ")";
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

    public static<D> KDColorTree<D> make(List<D> data, Function<D, Integer> colorExtractor, boolean useAlpha) {
        Random random = new Random();
        int dimension = useAlpha ? 4 : 3;
        int[] buffer = new int[getSampleSize(data.size())];
        Node<D> root = makeRecursively(random, dimension, data, colorExtractor, buffer, 0);
        return new KDColorTree<>(root);
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
