package nayuki.huffmancoding;

import java.io.*;


public final class AdaptiveHuffmanDecompress {

    public static void main(String[] args) throws IOException {
        // Show what command line arguments to use
        if (args.length == 0) {
            System.err.println("Usage: java AdaptiveHuffmanDecompress InputFile OutputFile");
            System.exit(1);
            return;
        }

        // Otherwise, decompress
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        BitInputStream in = new BitInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            decompress(in, out);
        } finally {
            out.close();
            in.close();
        }
    }


    static void decompress(BitInputStream in, OutputStream out) throws IOException {

        FrequencyTable freqTable = new FrequencyTable();
        HuffmanDecoder dec = new HuffmanDecoder(in);
        dec.codeTree = freqTable.buildCodeTree();
        int count = 0;
        while (true) {
            int symbol = dec.read();
            if (symbol == 256)  // EOF symbol
                break;
            out.write(symbol);

            freqTable.increment(symbol);
            count++;
            if (count < 262144 && isPowerOf2(count) || count % 262144 == 0)  // Update code tree
                dec.codeTree = freqTable.buildCodeTree();
            if (count % 262144 == 0)  // Reset frequency table
                freqTable = new FrequencyTable();
        }
    }


    private static boolean isPowerOf2(int x) {
        return x > 0 && (x & -x) == x;
    }

}
