package edu.jhu.cs639.test;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.roaringbitmap.RoaringBitmap;

import java.io.*;
import java.util.Random;

/**
 * Created by ankit on 1/23/15.
 */
public class BitmapGenomics {
    public static void main(String[] args) throws IOException {
        EWAHCompressedBitmap ewahBitmap1 = EWAHCompressedBitmap.bitmapOf(0, 2, 55, 64, 1 << 30);
        ewahBitmap1.clear();
        Random rand = new Random();
        RoaringBitmap roaringBitmap = new RoaringBitmap();
        for (int i = 0; i < 157; i += 4) {
            final int i1 = i + rand.nextInt(3);
            ewahBitmap1.set(i1);
            roaringBitmap.add(i1);
        }
        EWAHCompressedBitmap ewahBitmap2 = EWAHCompressedBitmap.bitmapOf(1, 3, 64,
                1 << 30);
//        System.out.println("bitmap 1: " + ewahBitmap1);
        System.out.println("bitmap 2: " + ewahBitmap2);
        // or
        // serialization
        FileOutputStream ewahSer = new FileOutputStream("test-files/testewah");
        FileOutputStream roarSer = new FileOutputStream("test-files/testroar");
        GzipCompressorOutputStream stream = new GzipCompressorOutputStream(ewahSer);
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Note: you could use a file output steam instead of ByteArrayOutputStream
        roaringBitmap.serialize(new DataOutputStream(roarSer));

        ewahBitmap1.serialize(new DataOutputStream(ewahSer));
        stream.close();
        ewahSer.close();
        roarSer.close();
        GzipCompressorInputStream stream1 = new GzipCompressorInputStream(new FileInputStream("test-files/testewah"));
        EWAHCompressedBitmap testMap = new EWAHCompressedBitmap();
        testMap.deserialize(new DataInputStream(stream1));
        System.out.println(testMap.equals(ewahBitmap1));
//        EWAHCompressedBitmap ewahBitmap1new = new EWAHCompressedBitmap();
//        byte[] bout = bos.toByteArray();
//        ewahBitmap1new.deserialize(new DataInputStream(new ByteArrayInputStream(bout)));
//        System.out.println("bitmap 1 (recovered) : " + ewahBitmap1new);
//        if (!ewahBitmap1.equals(ewahBitmap1new)) throw new RuntimeException("Will not happen");
        //
        // we can use a ByteBuffer as backend for a bitmap
        // which allows memory-mapped bitmaps
        //
//        ByteBuffer bb = ByteBuffer.wrap(bout);
//        EWAHCompressedBitmap rmap = new EWAHCompressedBitmap(bb);
//        System.out.println("bitmap 1 (mapped) : " + rmap);

        //
        // support for threshold function (new as of version 0.8.0):
        // mark as true a bit that occurs at least T times in the source
        // bitmaps
        //
    }
}
