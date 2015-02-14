package edu.jhu.cs639.test;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;
import nayuki.huffmancoding.BitOutputStream;

import java.io.*;
import java.util.*;

public class RLHCompression {

    public static void main(String[] args) throws IOException {
        final Map<String, EWAHCompressedBitmap> maps = new HashMap<String, EWAHCompressedBitmap>();
        long t = System.currentTimeMillis();

//        final String inputFile = "/Users/User1/Documents/classes/fall2014-classes/genomics/project/hg19.fa";
//        final String inputFile = "/Users/User1/Developer/asgc/test-files/non-scalce_read";
//        final String outputFilePrefix = "/Users/User1/Developer/asgc/test-files/assembly";
        int limit = Integer.parseInt(args[0]);
        final String inputFile = args[1];
        final String outputFilePrefix = args[2];
        final boolean isSingle = Boolean.parseBoolean(args[3]);
        final int kmerLength = Integer.parseInt(args[4]);

        final BufferedReader br = new BufferedReader(new FileReader(inputFile));
        char[] data = new char[limit];
        String inputData;
        int index = 0;
        while ((inputData = br.readLine()) != null) {
            if (inputData.startsWith(">")) {
                continue;
            }
            final char[] charArray = inputData.toLowerCase().toCharArray();
            for (char aCharArray : charArray) {
                data[index++] = aCharArray;
            }
            if (index + 50 >= data.length) {
                break;
            }
        }
        int padding = index % kmerLength;
        while (padding-- > 0) {
            data[index++] = 'n';
        }
        for (int i = 0; i + kmerLength <= index; i += kmerLength) {
            final String key = new String(Arrays.copyOfRange(data, i, i + kmerLength));
            EWAHCompressedBitmap bitmap = maps.get(key);
            if (bitmap == null) {
                bitmap = new EWAHCompressedBitmap();
                maps.put(key, bitmap);

            }
            bitmap.set(i / 3);
        }
        if (isSingle) {
            writeSingleRLHFile(maps, outputFilePrefix + "full-file");
        } else {
            for (final Map.Entry<String, EWAHCompressedBitmap> entry : maps.entrySet()) {
                writeRLHFile(entry.getValue(), outputFilePrefix + entry.getKey());
            }
        }
        System.out.println("time taken : " + (System.currentTimeMillis() - t) / 1000);
    }

    private static void writeSingleRLHFile(final Map<String, EWAHCompressedBitmap> allMaps, final String fileName) throws IOException {
        final Map<Integer, Integer> freq = new HashMap<Integer, Integer>();

        for (final EWAHCompressedBitmap map : allMaps.values()) {
            fillFreqMap(map, freq);
        }

        final HuffmanTree tree = HuffmanCode.buildTree(freq);
        final Map<Integer, String> codes = new HashMap<Integer, String>();

        HuffmanCode.getEncoding(tree, new StringBuilder(), codes);

        final BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));
        for (final EWAHCompressedBitmap map : allMaps.values()) {
            writeMap(map, codes, out);
        }
        out.output.write(codes.toString().getBytes());
        out.close();
    }

    private static void writeRLHFile(final EWAHCompressedBitmap bitmapA, final String fileName) throws IOException {
        final Map<Integer, Integer> freq = new HashMap<Integer, Integer>();
        fillFreqMap(bitmapA, freq);
        final HuffmanTree tree = HuffmanCode.buildTree(freq);

        final Map<Integer, String> codes = new HashMap<Integer, String>();

        HuffmanCode.getEncoding(tree, new StringBuilder(), codes);
        final BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));

        writeMap(bitmapA, codes, out);
        out.output.write(codes.toString().getBytes());
        out.close();
    }

    private static void writeMap(final EWAHCompressedBitmap bitmapA, final Map<Integer, String> codes,
                                 final BitOutputStream out) throws IOException {
        final IntIterator intIterator = bitmapA.intIterator();
        int begin = -1;
        while (intIterator.hasNext()) {
            int next = intIterator.next();
            int diff = next - begin - 1;
            final String s = codes.get(diff);
            for (char c : s.toCharArray()) {
                if (c == '1')
                    out.write(1);
                else
                    out.write(0);
            }
            begin = next;
        }
    }

    private static void fillFreqMap(final EWAHCompressedBitmap bitmapA, final Map<Integer, Integer> freq) {
        final IntIterator intIterator = bitmapA.intIterator();
        int begin = -1;
        while (intIterator.hasNext()) {
            int next = intIterator.next();
            int diff = next - begin - 1;
            Integer currentFreq = freq.get(diff);
            if (currentFreq == null) {
                currentFreq = 0;
            }
            freq.put(diff, ++currentFreq);
            begin = next;
        }
    }
}
