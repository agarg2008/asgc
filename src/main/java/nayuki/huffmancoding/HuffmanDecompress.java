package nayuki.huffmancoding;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Decompresses an input file that was compressed with HuffmanCompress, to an output file.
public final class HuffmanDecompress {

    public static void main(String[] args) throws IOException {
        // Show what command line arguments to use
        if (args.length == 0) {
            System.err.println("Usage: java HuffmanDecompress InputFile OutputFile");
            System.exit(1);
            return;
        }

        // Otherwise, decompress
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        BitInputStream in = new BitInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            CodeTree code = readCode(in);
            decompress(code, in, out);
        } finally {
            out.close();
            in.close();
        }
    }


    static CodeTree readCode(BitInputStream in) throws IOException {
        byte[] buffer = new byte[4];
        in.input.read(buffer);
        final int anInt = ByteBuffer.wrap(buffer).getInt(0);

        buffer = new byte[anInt];
        in.input.read(buffer);
        final Map<Integer, List<Integer>> convert = convert(new String(buffer));

        return new CodeTree(null, convert);
    }

    public static Map<Integer, List<Integer>> convert(String str) {
        String[] tokens = str.split(" |=");
        Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < tokens.length - 1; ) {
            final String token1 = tokens[i++];
            final String token2 = tokens[i++];
//            List<Inte>
//            map.put(Integer.parseInt(token1), Integer.parseInt(token2));
        }
        return map;
    }

    static void decompress(CodeTree code, BitInputStream in, OutputStream out) throws IOException {
        HuffmanDecoder dec = new HuffmanDecoder(in);
        dec.codeTree = code;
        while (true) {
            int symbol = dec.read();
            if (symbol == -1)  // EOF symbol
                break;
            out.write(symbol);
        }
    }

}
