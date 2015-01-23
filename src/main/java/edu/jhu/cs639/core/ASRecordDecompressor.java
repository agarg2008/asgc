package edu.jhu.cs639.core;

import edu.jhu.cs639.util.Constants;
import edu.jhu.cs639.datatype.ASBigRecord;
import edu.jhu.cs639.datatype.ASCompRecord;
import edu.jhu.cs639.datatype.CompactRead;

/**
 * This class takes ASCompactedRecord as an argument and returns an uncompressed record.
 */
public class ASRecordDecompressor {
    public static final String REGEX_ANY_CHAR = "[A-Z]+"; // REGEX used to identify all numerals.
    public static final String REGEX_ANY_NUMERAL = "\\d+"; // REGEX used to identify all bases.

    /**
     * This method takes compressed record in ASGC format and returns an uncompressed record.
     */
    public static ASBigRecord decompress(final ASCompRecord compressedRecord, final char[] referenceRead) {
        final String quality = compressedRecord.getQual();

        // We will assume read to be same as reference read and then we will apply mutations one by one.
        final StringBuilder read = new StringBuilder();
        read.append(referenceRead);

        final CompactRead compactRead = compressedRecord.getRead();
        if (compactRead != null) {
            final String misMatches = compactRead.getMismatch();
            final String deletions = compactRead.getDeletion();
            final String insertions = compactRead.getInsertion();
            if (misMatches != null) {// Handling mismatch.
                final String[] refIndex = misMatches.split(REGEX_ANY_CHAR);
                final String[] mismatches = misMatches.trim().split(REGEX_ANY_NUMERAL);
                for (int i = 0; i < refIndex.length; i++) {
                    int index = Integer.parseInt(refIndex[i]);
                    for (char c : mismatches[i + 1].toCharArray()) {
                        read.setCharAt(index, c);
                        index++;
                    }
                }
            }
            if (deletions != null) {//Handling deletion.
                final String[] splits = deletions.split(":");
                for (int i = 0; i < splits.length; i += 2) {
                    int start = Integer.parseInt(splits[i]);
                    int end = start + Integer.parseInt(splits[i + 1]) - 1;
                    for (int j = start; j <= end; j++) {
                        // Instead of deleting bases right away, we are replace them with a special symbol reserved for deleted
                        // chars. This is done to preserve the indices of deletion and insertions still to be taken care of.
                        read.setCharAt(j, Constants.HYPHEN);
                    }
                }
            }
            if (insertions != null) {// Handling insertion
                String[] refIndex = insertions.split(REGEX_ANY_CHAR);
                String[] insertionSplit = insertions.split(REGEX_ANY_NUMERAL);
                int insertionLength = 0;
                for (int i = 0; i < refIndex.length; i++) {
                    int index = Integer.parseInt(refIndex[i]);
                    read.insert(index + insertionLength, insertionSplit[i + 1]);
                    // We just inserted bases in reference genome as a result alignment indices related to reference genome
                    // has updated. We need to keep track of those updates.
                    insertionLength += insertionSplit[i + 1].length();
                }
            }
        }
        return new ASBigRecord(read.toString().replace("-", ""), decompressQuality(quality));
    }

    /**
     * Decompressed the given compressed quality string.
     */
    public static String decompressQuality(final String quality) {
        try {
            final char[] charArray = quality.toCharArray();
            char previousChar = charArray[0];
            charArray[0] = getChar(previousChar);
            for (int i = 1; i < charArray.length; i++) {
                char newChar = (char) (charArray[i] + previousChar);
                charArray[i] = getChar(newChar);
                previousChar = newChar;
            }
            return new String(charArray);
        } catch (Exception e) {
            final String errorMessage = "Unexpected quality value encountered : " + quality;
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Small utility method for reverse lookup and offset.
     */
    private static char getChar(final char c) {
        final int newChar = Constants.QUALITY_DECOMPRESSION_MAP.get(c);
        return (char) (newChar + Constants.PHRED_CONSTANT);
    }
}
