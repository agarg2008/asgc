package edu.jhu.cs639.test;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RLHCompression {

    public static void main(String[] args) throws IOException {
        final Map<String, EWAHCompressedBitmap> maps = new HashMap<String, EWAHCompressedBitmap>();
        long t = System.currentTimeMillis();

        final String inputFile = "/Users/User1/Developer/asgc/test-files/non-scalce_read";
        final String outputFilePrefix = "test-files/4scbwt-testewah";

        final BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line;
        int index = 0;
        final StringBuilder reads = new StringBuilder();
        while ((line = br.readLine()) != null) {
            reads.append(line);
        }
        line = reads.toString();
        int length = line.length();
        final int kmerLength = 1;
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
        for (final Map.Entry<String, EWAHCompressedBitmap> entry : maps.entrySet()) {
            writeRLHFile(entry.getValue(), outputFilePrefix + entry.getKey());
        }
        System.out.println("time taken : " + (System.currentTimeMillis() - t) / 1000);
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
        final StringBuilder sb = new StringBuilder();
        for (int i : diffList) {
            sb.append(codes.get(i));
        }
        final String encoded = sb.toString();
        try {
            final BitWriter bw = new BitWriter(encoded.length());
            for (int i = 0; i < encoded.length(); i++) {
                bw.writeBit(encoded.charAt(i) == '1');
            }

            byte[] b = bw.toArray();

            final FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(codes.toString().getBytes());
            fos.write(b);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
