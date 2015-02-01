package nayuki.huffmancoding;

import java.io.*;
import java.nio.ByteBuffer;


// Uses static Huffman coding to compress an input file to an output file. Use HuffmanDecompress to decompress.
// Uses 257 symbols - 256 for byte values and 1 for EOF. The compressed file format contains the code length of each symbol under a canonical code, followed by the Huffman-coded data.
public final class HuffmanCompress {

    public static void main(String[] args) throws IOException {
        long t = System.currentTimeMillis();
        // Show what command line arguments to use
        if (args.length == 0) {
            System.err.println("Usage: java HuffmanCompress InputFile OutputFile");
            System.exit(1);
            return;
        }
        // Otherwise, compress
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        // Read input file once to compute symbol frequencies
        // The resulting generated code is optimal for static Huffman coding and also canonical
        FrequencyTable freq = getFrequencies(inputFile);
        freq.increment(-1);  // EOF symbol gets a frequency of 1
        CodeTree code = freq.buildCodeTree();

        // Read input file again, compress with Huffman coding, and write output file
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        try {
            writeCode(out, code);
            compress(code, in, out);
        } finally {
            out.close();
            in.close();
        }
        System.out.println("Time taken : " + (System.currentTimeMillis() - t) / 1000);
    }


    private static FrequencyTable getFrequencies(File file) throws IOException {
        FrequencyTable freq = new FrequencyTable();
        InputStream input = new BufferedInputStream(new FileInputStream(file));
        try {
            while (true) {
                int b = input.read();
                if (b == -1)
                    break;
                freq.increment(b);
            }
        } finally {
            input.close();
        }
        return freq;
    }


    static void writeCode(BitOutputStream out, CodeTree canonCode) throws IOException {
        final byte[] bytes = canonCode.getCodes().toString().getBytes();
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(bytes.length);
        out.output.write(bb.array());
        out.output.write(bytes);
    }


    static void compress(CodeTree code, InputStream in, BitOutputStream out) throws IOException {
        HuffmanEncoder enc = new HuffmanEncoder(out);
        enc.codeTree = code;
        while (true) {
            int b = in.read();
            if (b == -1)
                break;
            enc.write(b);
        }
        enc.write(-1); // EOF
    }

}
