package nayuki.huffmancoding;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * A table of symbol frequencies. Mutable.
 */
/*
 * A FrequencyTable is mainly used like this:
 * - Collect the frequencies of symbols in the stream that we want to compress.
 * - Build a code tree that is statically optimal for the current frequencies.
 * This implementation correctly builds an optimal code tree for any legal number of symbols (2 to Integer.MAX_VALUE), with each symbol having a legal frequency (0 to Integer.MAX_VALUE). It is designed not to err due to overflow.
 */
public final class FrequencyTable {

    private Map<Integer, Integer> frequencies;

    public FrequencyTable() {
        frequencies = new HashMap<Integer, Integer>();
    }


    public int getSymbolLimit() {
        return frequencies.size();
    }


    public int get(int symbol) {
        return frequencies.get(symbol);
    }


    public void set(int symbol, int freq) {
        frequencies.put(symbol, freq);
    }


    public void increment(int symbol) {
        Integer currentFreq = frequencies.get(symbol);
        if (currentFreq == null) {
            currentFreq = 0;
        }
        frequencies.put(symbol, ++currentFreq);
    }


    // Returns a string showing all the symbols and frequencies. The format is subject to change. Useful for debugging.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (final Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
            sb.append(String.format("%d\t%d%n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }


    // Returns a code tree that is optimal for these frequencies. Always contains at least 2 symbols, to avoid degenerate trees.
    public CodeTree buildCodeTree() {
        // Note that if two nodes have the same frequency, then the tie is broken by which tree contains the lowest symbol. Thus the algorithm is not dependent on how the queue breaks ties.
        Queue<NodeWithFrequency> pqueue = new PriorityQueue<NodeWithFrequency>();

        // Add leaves for symbols with non-zero frequency
        for (final Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
            pqueue.add(new NodeWithFrequency(new Leaf(entry.getKey()), entry.getKey(), entry.getValue()));
        }

        // Pad with zero-frequency symbols until queue has at least 2 items
//        for (int i = 0; i < frequencies.length && pqueue.size() < 2; i++) {
//            if (i >= frequencies.length || frequencies[i] == 0)
//                pqueue.add(new NodeWithFrequency(new Leaf(i), i, 0));
//        }
        if (pqueue.size() < 2)
            throw new AssertionError();

        // Repeatedly tie together two nodes with the lowest frequency
        while (pqueue.size() > 1) {
            NodeWithFrequency nf1 = pqueue.remove();
            NodeWithFrequency nf2 = pqueue.remove();
            pqueue.add(new NodeWithFrequency(
                    new InternalNode(nf1.node, nf2.node),
                    Math.min(nf1.lowestSymbol, nf2.lowestSymbol),
                    nf1.frequency + nf2.frequency));
        }

        // Return the remaining node
        return new CodeTree((InternalNode) pqueue.remove().node);
    }


    private static class NodeWithFrequency implements Comparable<NodeWithFrequency> {

        public final Node node;

        public final int lowestSymbol;

        public final long frequency;  // Using long prevents overflow


        public NodeWithFrequency(Node node, int lowestSymbol, long freq) {
            this.node = node;
            this.lowestSymbol = lowestSymbol;
            this.frequency = freq;
        }


        public int compareTo(NodeWithFrequency other) {
            if (frequency < other.frequency)
                return -1;
            else if (frequency > other.frequency)
                return 1;
            else if (lowestSymbol < other.lowestSymbol)
                return -1;
            else if (lowestSymbol > other.lowestSymbol)
                return 1;
            else
                return 0;
        }

    }

}
