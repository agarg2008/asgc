package edu.jhu.cs639.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a utility class to keep constants.
 */
public class Constants {

    // This is the suffix of compressed file containing aligned reads.
    public static final String SUFFIX_ALIGNED = "_aligned";

    // This is the suffix of compressed file containing unaligned reads.
    public static final String SUFFIX_UNALIGNED = "_unaligned";

    public static final int PHRED_CONSTANT = 33;
    public static final char WORST_QUALITY = '\u0006';
    public static final char HYPHEN = '-';

    // This is the lookup for the bins when additionally compressing quality scores using bin.
    public static final Map<Integer, Character> QUALITY_COMPRESSION_MAP = new HashMap<Integer, Character>();

    // This is the reverse lookup for the bins while decompressing when it was additionally compressed using bins.
    public static final Map<Character, Integer> QUALITY_DECOMPRESSION_MAP = new HashMap<Character, Integer>();

    static {
        fillCompressionMap(2, 9, '\u0000');
        fillCompressionMap(10, 19, '\u0001');
        fillCompressionMap(20, 24, '\u0002');
        fillCompressionMap(25, 29, '\u0003');
        fillCompressionMap(30, 34, '\u0004');
        fillCompressionMap(35, 39, '\u0005');

        QUALITY_DECOMPRESSION_MAP.put('\u0000', 6);
        QUALITY_DECOMPRESSION_MAP.put('\u0001', 15);
        QUALITY_DECOMPRESSION_MAP.put('\u0002', 22);
        QUALITY_DECOMPRESSION_MAP.put('\u0003', 27);
        QUALITY_DECOMPRESSION_MAP.put('\u0004', 33);
        QUALITY_DECOMPRESSION_MAP.put('\u0005', 37);
        QUALITY_DECOMPRESSION_MAP.put('\u0006', 40);
    }

    private static void fillCompressionMap(int start, int end, char c) {
        for (int i = start; i <= end; i++) {
            QUALITY_COMPRESSION_MAP.put(i, c);
        }
    }
}
