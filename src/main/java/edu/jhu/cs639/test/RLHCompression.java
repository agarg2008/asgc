package edu.jhu.cs639.test;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;
import nayuki.huffmancoding.BitOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RLHCompression {

    public static void main(String[] args) throws IOException {
        final Map<String, EWAHCompressedBitmap> maps = new HashMap<String, EWAHCompressedBitmap>();
        long t = System.currentTimeMillis();

        final String inputFile = "/Users/User1/Developer/asgc/test-files/non-scalce_read";
        final String outputFilePrefix = "/Users/User1/Developer/asgc/test-files/full4scbwt-testewah";
        final int kmerLength = 3;

        final BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line;
        int index = 0;
        final StringBuilder reads = new StringBuilder();
        while ((line = br.readLine()) != null) {
            reads.append(line);
        }
        line = reads.toString();
        int length = line.length();
        int padding = length % kmerLength;
        length += padding;
        while (padding-- > 0) {
            line = line + "N";
        }
        for (int i = 0; i + kmerLength <= length; i += kmerLength) {
            final String key = line.substring(i, i + kmerLength);
            EWAHCompressedBitmap bitmap = maps.get(key);
            if (bitmap == null) {
                bitmap = new EWAHCompressedBitmap();
                maps.put(key, bitmap);

            }
            bitmap.set(index);
            index += kmerLength;
        }
        if (false) {
            writeSingleRLHFile(maps, outputFilePrefix + "full-file");
        } else {
            for (final Map.Entry<String, EWAHCompressedBitmap> entry : maps.entrySet()) {
                writeRLHFile(entry.getValue(), outputFilePrefix + entry.getKey());
            }
        }
        System.out.println("time taken : " + (System.currentTimeMillis() - t) / 1000);
    }

    private static void writeSingleRLHFile(final Map<String, EWAHCompressedBitmap> allMaps, final String fileName) throws IOException {
        final List<Integer> diffList = new ArrayList<Integer>();
        final Map<Integer, Integer> freq = new HashMap<Integer, Integer>();

        for (final Map.Entry<String, EWAHCompressedBitmap> entry : allMaps.entrySet()) {
            final IntIterator intIterator = entry.getValue().intIterator();
            int begin = -1;
            while (intIterator.hasNext()) {
                int next = intIterator.next();
                int diff = next - begin - 1;
                Integer currentFreq = freq.get(diff);
                if (currentFreq == null) {
                    currentFreq = 0;
                }
                freq.put(diff, ++currentFreq);
                diffList.add(diff);
                begin = next;
            }
        }

        final HuffmanTree tree = HuffmanCode.buildTree(freq);
        final Map<Integer, String> codes = new HashMap<Integer, String>();

        HuffmanCode.getEncoding(tree, new StringBuilder(), codes);

        final BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));
        for (int i : diffList) {
            final String s = codes.get(i);
            for (char c : s.toCharArray()) {
                if (c == '1')
                    out.write(1);
                else
                    out.write(0);
            }
        }
        out.output.write(codes.toString().getBytes());
        out.close();
    }

    private static void writeRLHFile(final EWAHCompressedBitmap bitmapA, final String fileName) throws IOException {
        final IntIterator intIterator = bitmapA.intIterator();
        int begin = -1;
        final Map<Integer, Integer> freq = new HashMap<Integer, Integer>();
        final List<Integer> diffList = new ArrayList<Integer>();
        while (intIterator.hasNext()) {
            int next = intIterator.next();
            int diff = next - begin - 1;
            Integer currentFreq = freq.get(diff);
            if (currentFreq == null) {
                currentFreq = 0;
            }
            freq.put(diff, ++currentFreq);
            diffList.add(diff);
            begin = next;
        }
        final HuffmanTree tree = HuffmanCode.buildTree(freq);

        Map<Integer, String> codes = new HashMap<Integer, String>();

        HuffmanCode.getEncoding(tree, new StringBuilder(), codes);
        BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));

        for (int i : diffList) {
            final String s = codes.get(i);
            for (char c : s.toCharArray()) {
                if (c == '1')
                    out.write(1);
                else
                    out.write(0);
            }
        }
        out.output.write(codes.toString().getBytes());
        out.close();
    }
}
