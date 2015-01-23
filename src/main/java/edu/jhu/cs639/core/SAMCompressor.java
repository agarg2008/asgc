package edu.jhu.cs639.core;

import edu.jhu.cs639.util.Constants;
import edu.jhu.cs639.datatype.ASCompRecord;
import edu.jhu.cs639.datatype.CompactRead;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import java.util.List;

/**
 * This class takes a read as SAMRecord and alignment from assembly and returns a compact representation of the read.
 */
public class SAMCompressor {

    /**
     * This method takes a sam-record as an input, reference string from genome and alignment index (adjusted by previous alignment)
     * and returns a compressed record in ASGC format.
     */
    public static ASCompRecord compressSAM(final SAMRecord samRecord, final char[] referenceString, final int adjustedAlignStart) {
        final Cigar cigar = samRecord.getCigar();
        final int referenceLength = cigar.getReferenceLength(); // This is required to make decompression faster.

        final String readString = samRecord.getReadString();
        final String qualityString = samRecord.getBaseQualityString();

        final List<CigarElement> cigarElements = cigar.getCigarElements();

        // Will have a variable to track index in reference and another to track index in reads.
        // this index starts from zero as it is relative to adjusted alignment-start.
        int referenceIndex = 0;
        // read index is required to keep track of mismatches and insertions
        int readIndex = 0;

        final StringBuilder mismatchBuilder = new StringBuilder();
        final StringBuilder deletionBuilder = new StringBuilder();
        final StringBuilder insertionBuilder = new StringBuilder();

        for (final CigarElement element : cigarElements) {
            // We will iterate though each cigar element at a time and capture the mismatch/insertion/deletion represented
            // by that element.
            final CigarOperator operator = element.getOperator();
            final int operationLength = element.getLength();
            switch (operator) {
                case M: // For element 'M' we can have either mismatch or match at the
                    boolean isFirst = true;
                    for (int i = 0; i < operationLength; i++) {
                        final char readChar = readString.charAt(readIndex);
                        if (readChar != referenceString[referenceIndex]) {
                            if (isFirst) {
                                mismatchBuilder.append(referenceIndex);
                                isFirst = false;
                            }
                            mismatchBuilder.append(readChar);
                        } else {
                            isFirst = true;
                        }
                        readIndex++;
                        referenceIndex++;
                    }
                    break;
                case D:
                    deletionBuilder.append(referenceIndex).append(":").append(operationLength).append(":");
                    referenceIndex += operationLength; // Only reference index is increasing, no change in readIndex.
                    break;
                case I:
                    insertionBuilder.append(referenceIndex);
                    insertionBuilder.append(readString.substring(readIndex, readIndex + operationLength));
                    readIndex += operationLength; // Only readIndex is increasing, no change in referenceIndex.
                    break;
                case EQ: // We don't need to do anything special when there is a match.
                    referenceIndex++;
                    readIndex++;
                    break;
                case X:
                    mismatchBuilder.append(referenceIndex);
                    mismatchBuilder.append(readString.substring(readIndex, readIndex + operationLength));
                    readIndex += operationLength;
                    referenceIndex += operationLength;
                    break;
                default:
                    throw new RuntimeException("Unexpected Cigar operator found : " + operator);
            }
        }

        boolean isExactRead = true;
        final CompactRead.Builder compactRead = CompactRead.newBuilder();
        if (mismatchBuilder.length() > 0) {
            isExactRead = false;
            compactRead.setMismatch(mismatchBuilder.toString());
        }
        if (deletionBuilder.length() > 0) {
            isExactRead = false;
            compactRead.setDeletion(deletionBuilder.toString());
        }
        if (insertionBuilder.length() > 0) {
            isExactRead = false;
            compactRead.setInsertion(insertionBuilder.toString());
        }

        final ASCompRecord.Builder compactedRecord = ASCompRecord.newBuilder();
        compactedRecord.setStart(adjustedAlignStart);
        compactedRecord.setReferenceLength(referenceLength);
        if (!isExactRead) { // Set this field only when there is an actual difference from the reference.
            compactedRecord.setRead(compactRead.build());
        }
        compactedRecord.setQual(compressQuality(qualityString));
        compactedRecord.setRegion(samRecord.getReferenceName());
        return compactedRecord.build();
    }

    /**
     * Given a quality string, we compress it by first approximating quality into bins and then keeping the difference
     * in quality from the base left to the current base.
     */
    public static String compressQuality(final String quality) {
        final char[] charArray = quality.toCharArray();
        char previousChar = getChar(charArray[0]);
        charArray[0] = previousChar;
        for (int i = 1; i < charArray.length; i++) {
            char newChar = getChar(charArray[i]);
            charArray[i] = (char) (newChar - previousChar);
            previousChar = newChar;
        }
        return new String(charArray);
    }

    private static char getChar(final char c) {
        int pastChar = (int) c - Constants.PHRED_CONSTANT;
        Character pastChar1 = Constants.QUALITY_COMPRESSION_MAP.get(pastChar);
        if (pastChar1 == null) {
            pastChar1 = Constants.WORST_QUALITY;
        }
        return pastChar1;
    }
}
