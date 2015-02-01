package edu.jhu.cs639.test;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.util.*;

public class EWAHCompressTest {

    public static void main(String[] args) throws IOException {
        Map<String, EWAHCompressedBitmap> maps = new HashMap<String, EWAHCompressedBitmap>();
        long t = System.currentTimeMillis();

        BufferedReader br = new BufferedReader(new FileReader("/Users/User1/Developer/asgc/test-files/scalce_read"));
        String line;
        int index = 0;
        StringBuilder reads = new StringBuilder();
        while ((line = br.readLine()) != null) {
            reads.append(line);
        }
        {
            line = reads.toString();
            int length = line.length();
            int i2 = length % 3;
            length += i2;
            while (i2-- > 0) {
                line = line + "N";
            }
            final int i1 = 3;
            for (int i = 0; i + i1 <= length; i += i1) {
                String key = line.substring(i, i + i1);
                EWAHCompressedBitmap bitmap = maps.get(key);
                if (bitmap == null) {
                    bitmap = new EWAHCompressedBitmap();
                    maps.put(key, bitmap);

                }
                bitmap.set(index);
                index += i1;
            }
        }
        for (final Map.Entry<String, EWAHCompressedBitmap> entry : maps.entrySet()) {
            writeRLHFile(entry.getValue(), "test-files/5scbwt-testewah" + entry.getKey());
        }
        System.out.println("time taken : " + (System.currentTimeMillis() - t) / 1000);
    }

    private static void writeRLHFile(EWAHCompressedBitmap bitmapA, String fileName) throws IOException {
        final IntIterator intIterator = bitmapA.intIterator();
        int begin = -1;
        Map<Integer, Integer> freq = new HashMap<Integer, Integer>();
        List<Integer> diffList = new ArrayList<Integer>();
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
//        System.out.println("Cardinality of bitmap : " + bitmapA.cardinality() + "; size of bitmap : " + begin);

        HuffmanTree tree = HuffmanCode.buildTree(freq);

        // print out results
//        System.out.println("SYMBOL\tWEIGHT\tHUFFMAN CODE");
        Map<Integer, String> codes = new HashMap<Integer, String>();

        HuffmanCode.printCodes(tree, new StringBuilder(), codes);
        StringBuilder sb = new StringBuilder();
        for (int i : diffList) {
            sb.append(codes.get(i));
        }
        final String encoded = sb.toString();
//        System.out.println("length of string : " + encoded.length());
        try {

            BitWriter bw = new BitWriter(encoded.length());
            for (int i = 0; i < encoded.length(); i++) {
                bw.writeBit(encoded.charAt(i) == '1');
            }

            byte[] b = bw.toArray();

            FileOutputStream ewahSer = new FileOutputStream(fileName);
//            GzipCompressorOutputStream stream = new GzipCompressorOutputStream(ewahSer);
            ewahSer.write(codes.toString().getBytes());
            ewahSer.write(b);
//            bitmapA.serialize(new DataOutputStream(stream));
//            stream.close();
            ewahSer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeFile(EWAHCompressedBitmap32 bitmapA, String fileName) throws IOException {
        try {
            FileOutputStream ewahSer = new FileOutputStream(fileName);
            GzipCompressorOutputStream stream = new GzipCompressorOutputStream(ewahSer);
            bitmapA.serialize(new DataOutputStream(stream));
            stream.close();
            ewahSer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fillKey(final Set<String> keys) {
        keys.add("A");
        keys.add("T");
        keys.add("C");
        keys.add("G");
        keys.add("N");
        keys.add("AA");
        keys.add("AT");
        keys.add("AC");
        keys.add("AG");
        keys.add("AN");
        keys.add("TA");
        keys.add("TT");
        keys.add("TC");
        keys.add("TG");
        keys.add("TN");
        keys.add("CA");
        keys.add("CT");
        keys.add("CC");
        keys.add("CG");
        keys.add("CN");
        keys.add("GA");
        keys.add("GT");
        keys.add("GC");
        keys.add("GG");
        keys.add("GN");
        keys.add("NA");
        keys.add("NT");
        keys.add("NC");
        keys.add("NG");
        keys.add("NN");
    }
}
