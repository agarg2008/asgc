package nayuki.huffmancoding;

import java.util.*;


/**
 * A canonical Huffman code. Immutable. Code length 0 means no code.
 */
/*
 * A canonical Huffman code only describes the code length of each symbol. The codes can be reconstructed from this information. In this implementation, symbols with lower code lengths, breaking ties by lower symbols, are assigned lexicographically lower codes.
 * Example:
 *   Code lengths (canonical code):
 *     Symbol A: 1
 *     Symbol B: 3
 *     Symbol C: 0 (no code)
 *     Symbol D: 2
 *     Symbol E: 3
 *   Huffman codes (generated from canonical code):
 *     Symbol A: 0
 *     Symbol B: 110
 *     Symbol C: None
 *     Symbol D: 10
 *     Symbol E: 111
 */
public final class CanonicalCode {


    private Map<Integer, Integer> codeLengths;


    // The constructor does not check that the array of code lengths results in a complete Huffman tree, being neither underfilled nor overfilled.
    public CanonicalCode() {
        codeLengths = new HashMap<Integer, Integer>();
    }


    // Builds a canonical code from the given code tree.
    public CanonicalCode(CodeTree tree) {
        codeLengths = new HashMap<Integer, Integer>();
        buildCodeLengths(tree.root, 0);
    }


    private void buildCodeLengths(Node node, int depth) {
        if (node instanceof InternalNode) {
            InternalNode internalNode = (InternalNode) node;
            buildCodeLengths(internalNode.leftChild, depth + 1);
            buildCodeLengths(internalNode.rightChild, depth + 1);
        } else if (node instanceof Leaf) {
            int symbol = ((Leaf) node).symbol;
//            if (codeLengths.get(symbol) != 0)
//                throw new AssertionError("Symbol has more than one code");  // Because CodeTree has a checked constraint that disallows a symbol in multiple leaves
//            if (symbol >= codeLengths.length)
//                throw new IllegalArgumentException("Symbol exceeds symbol limit");
            codeLengths.put(symbol, depth);
        } else {
            throw new AssertionError("Illegal node type");
        }
    }


//    public int getSymbolLimit() {
//        return max(codeLengths.keySet());
//    }

    public Map<Integer, Integer> getCodeLengths() {
        return codeLengths;
    }


    public int getCodeLength(int symbol) {
//        if (symbol < 0 || symbol >= codeLengths.length)
//            throw new IllegalArgumentException("Symbol out of range");
        return codeLengths.get(symbol);
    }


    public CodeTree toCodeTree() {
        List<Node> nodes = new ArrayList<Node>();
        for (int i = max(codeLengths.values()); i >= 1; i--) {  // Descend through positive code lengths
            List<Node> newNodes = new ArrayList<Node>();

            // Add leaves for symbols with code length i
            for (Map.Entry<Integer, Integer> entry : codeLengths.entrySet()) {
                if (entry.getValue() == i)
                    newNodes.add(new Leaf(entry.getKey()));
            }
//            for (int j = 0; j < codeLengths.length; j++) {
//                if (codeLengths[j] == i)
//                    newNodes.add(new Leaf(j));
//            }

            // Merge nodes from the previous deeper layer
            for (int j = 0; j < nodes.size(); j += 2)
                newNodes.add(new InternalNode(nodes.get(j), nodes.get(j + 1)));

            nodes = newNodes;
            if (nodes.size() % 2 != 0)
                throw new IllegalStateException("This canonical code does not represent a Huffman code tree");
        }

        if (nodes.size() != 2)
            throw new IllegalStateException("This canonical code does not represent a Huffman code tree");
        return new CodeTree(new InternalNode(nodes.get(0), nodes.get(1)));
    }


    private static int max(Collection<Integer> array) {
        int result = -1;
        for (int x : array)
            result = Math.max(x, result);
        return result;
    }

}
