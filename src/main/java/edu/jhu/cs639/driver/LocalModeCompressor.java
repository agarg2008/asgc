package edu.jhu.cs639.driver;

import edu.jhu.cs639.core.SAMCompressor;
import edu.jhu.cs639.datatype.ASBigRecord;
import edu.jhu.cs639.datatype.ASCompRecord;
import edu.jhu.cs639.util.AssemblyUtils;
import edu.jhu.cs639.util.Constants;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;
import org.apache.hadoop.fs.Path;
import parquet.avro.AvroParquetWriter;
import parquet.hadoop.ParquetWriter;
import parquet.hadoop.metadata.CompressionCodecName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This is our driver class for compression using ASGC format.
 */
public class LocalModeCompressor {

    private static String previousRegion = "";
    private static int previousAlignment = 0; // This is to be used while compressing data.
    private static final Map<String, char[]> assembly = new HashMap<String, char[]>();

    /**
     * This is the entry point for the code. This takes three parameters : <assembly-file> <input-samFile> <output-prefix>.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Please provide three arguments : <assembly-file> <input-samFile> <output-prefix>");
            System.exit(1);
        }
        final String assemblyFilePath = args[0].trim(); // This is the path to the reference genome assembly.
        final String inputFilePath = args[1].trim(); // This is the path to sorted SAM file (aligned + unaligned reads).
        // This is the prefix using which we will write files for aligned and unaligned reads.
        final String outputFilePrefix = args[2].trim();

        System.out.println("Reading assembly file from : " + assemblyFilePath);
        AssemblyUtils.readAssembly(assemblyFilePath, assembly);
        System.out.println("Finished reading assembly file.");

        compressFile(inputFilePath, outputFilePrefix);
    }

    /**
     * Internal method which takes input/output file parameters from the driver and writes compressed files for both
     * aligned and unaligned reads.
     */
    private static void compressFile(final String inputFilePath, final String outputFilePath)
            throws IOException {
        long alignedRecordRead = 0;
        long unalignedRecordRead = 0;

        // This is the output file where we will write compressed aligned reads.
        final Path alignedReadFile = new Path(new File(outputFilePath + Constants.SUFFIX_ALIGNED).getPath());
        final AvroParquetWriter<ASCompRecord> alignedWriter =
                new AvroParquetWriter<ASCompRecord>(alignedReadFile, ASCompRecord.getClassSchema(), CompressionCodecName.GZIP,
                        ParquetWriter.DEFAULT_BLOCK_SIZE, ParquetWriter.DEFAULT_PAGE_SIZE);

        // This is the output file where we will write compressed unaligned reads.
        final Path unalignedReadFile = new Path(new File(outputFilePath + Constants.SUFFIX_UNALIGNED).getPath());
        final AvroParquetWriter<ASBigRecord> unalignedWriter =
                new AvroParquetWriter<ASBigRecord>(unalignedReadFile, ASBigRecord.getClassSchema(), CompressionCodecName.GZIP,
                        ParquetWriter.DEFAULT_BLOCK_SIZE, ParquetWriter.DEFAULT_PAGE_SIZE);

        // Setting this as silent as we do not have any use of SAMHeaders and do not care if its missing.
        SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);
        final SAMFileReader samFileReader = new SAMFileReader(new FileInputStream(inputFilePath));
        for (final SAMRecord next : samFileReader) {
            int alignmentStart = next.getAlignmentStart();
            if (alignmentStart > 0) { // This is a mapped read.
                try {
                    final ASCompRecord compressedRecord = compressAlignedReads(next);
                    alignedWriter.write(compressedRecord);
                } catch (Exception e) {
                    System.out.println(next.getAlignmentStart());
                    System.out.println(next.getReadString());
                    System.out.println(next);
                    throw new RuntimeException(e);
                }
                if (++alignedRecordRead % 100000 == 0) {
                    System.out.println("Aligned records read : " + alignedRecordRead);
                }
            } else { // This is an unmapped read.
                // For the unaligned read, we are still compressing its quality value and writing as columnar format
                // because it gives nice compression.
                final ASBigRecord record = new ASBigRecord(next.getReadString(), SAMCompressor.compressQuality(next.getBaseQualityString()));
                unalignedWriter.write(record);
                if (++unalignedRecordRead % 100000 == 0) {
                    System.out.println("Unaligned records read : " + unalignedRecordRead);
                }
            }
        }

        alignedWriter.close();
        unalignedWriter.close();
        System.out.println("Total aligned records read : " + alignedRecordRead);
        System.out.println("Total unaligned records read : " + unalignedRecordRead);
    }

    private static ASCompRecord compressAlignedReads(final SAMRecord samRecord) {
        final String region = samRecord.getReferenceName();
        if (!region.equalsIgnoreCase(previousRegion)) {
            previousAlignment = 0;
            previousRegion = region;
        }
        final int alignmentStart = samRecord.getAlignmentStart();
        final Cigar cigar = samRecord.getCigar();
        final int referenceLength = cigar.getReferenceLength();
        final char[] referenceString = Arrays.copyOfRange(assembly.get(region), alignmentStart - 1, alignmentStart - 1 + referenceLength);
        final int adjustedStart = alignmentStart - previousAlignment;
        previousAlignment = alignmentStart;
        return SAMCompressor.compressSAM(samRecord, referenceString, adjustedStart);
    }
}
