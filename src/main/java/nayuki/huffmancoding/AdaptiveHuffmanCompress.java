package nayuki.huffmancoding;

import java.io.*;


public final class AdaptiveHuffmanCompress {

    public static void main(String[] args) throws IOException {
        // Show what command line arguments to use
        if (args.length == 0) {
            System.err.println("Usage: java AdaptiveHuffmanCompress InputFile OutputFile");
            System.exit(1);
            return;
        }

        // Otherwise, compress
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        try {
            compress(in, out);
        } finally {
            out.close();
            in.close();
        }
    }


    static void compress(InputStream in, BitOutputStream out) throws IOException {

        FrequencyTable freqTable = new FrequencyTable();
        HuffmanEncoder enc = new HuffmanEncoder(out);
        enc.codeTree = freqTable.buildCodeTree();  // We don't need to make a canonical code since we don't transmit the code tree
        int count = 0;
        while (true) {
            int b = in.read();
            if (b == -1)
                break;
            enc.write(b);

            freqTable.increment(b);
            count++;
            if (count < 262144 && isPowerOf2(count) || count % 262144 == 0)  // Update code tree
                enc.codeTree = freqTable.buildCodeTree();
            if (count % 262144 == 0)  // Reset frequency table
                freqTable = new FrequencyTable();
        }
        enc.write(256);  // EOF
    }


    private static boolean isPowerOf2(int x) {
        return x > 0 && (x & -x) == x;
    }

}
