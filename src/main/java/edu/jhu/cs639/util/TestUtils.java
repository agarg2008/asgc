package edu.jhu.cs639.util;

import edu.jhu.cs639.core.ASRecordDecompressor;
import edu.jhu.cs639.core.SAMCompressor;
import edu.jhu.cs639.datatype.ASBigRecord;
import edu.jhu.cs639.datatype.ASCompRecord;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the class one should use to evaluate SAGC compression-uncompression tool.
 * This takes a sorted SAM file as an input and performs compression and uncompressions and validates each output against
 * each input record.
 */
public class TestUtils {

    private static final Map<String, char[]> assembly = new HashMap<String, char[]>();

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Please provide two arguments : <assembly-file> <input-samFile>");
        }
        final String assemblyFilePath = args[0].trim(); // This is the path to the reference genome assembly.
        final String inputFilePath = args[1].trim(); // This is the path to sorted SAM file (aligned + unaligned reads).

        System.out.println("Reading assembly file from : " + assemblyFilePath);
        AssemblyUtils.readAssembly(assemblyFilePath, assembly);
        System.out.println("Finished reading assembly file.");

        testCompressionDecompression(inputFilePath);
    }

    private static void testCompressionDecompression(final String inputFilePath) throws FileNotFoundException {
        long recordRead = 0;

        // Setting this as silent as we do not have any use of SAMHeaders and do not care if its missing.
        SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);
        final SAMFileReader samFileReader = new SAMFileReader(new FileInputStream(inputFilePath));
        ASCompRecord compressedRecord;
        ASBigRecord uncompressedRecord;
        for (final SAMRecord next : samFileReader) {
            int alignmentStart = next.getAlignmentStart();
            if (alignmentStart > 0) { // This is a mapped read.
                final String region = next.getReferenceName();
                final Cigar cigar = next.getCigar();
                final int referenceLength = cigar.getReferenceLength();
                final char[] referenceString = Arrays.copyOfRange(assembly.get(region), alignmentStart - 1, alignmentStart - 1 + referenceLength);
                compressedRecord = SAMCompressor.compressSAM(next, referenceString, alignmentStart);
                uncompressedRecord = ASRecordDecompressor.decompress(compressedRecord, referenceString);

                // Validating if uncompressed record is same as compressed record.
                validateAlignedRecord(next, uncompressedRecord);
            } else {
                // This was an unaligned read. For unaligned reads we are only compressing the quality. And we know that we have
                // approximation in quality score. Thus not validating anything here.
            }
            if (++recordRead % 100000 == 0) {
                System.out.println("Validated records : " + recordRead);
            }
        }
    }

    private static void validateAlignedRecord(final SAMRecord samRecord, final ASBigRecord uncompressedRecord) {
        final String originalRead = samRecord.getReadString();
        final String recoveredRead = uncompressedRecord.getRead();
        boolean match = originalRead.equalsIgnoreCase(recoveredRead);
        if (!match) {
            System.out.println("Original read  : " + originalRead);
            System.out.println("Recovered read : " + recoveredRead);
            throw new RuntimeException("SamRecord and uncompressed read not matching.");
        }
    }
}

