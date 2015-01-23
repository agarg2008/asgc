package edu.jhu.cs639.driver;

import edu.jhu.cs639.core.ASRecordDecompressor;
import edu.jhu.cs639.datatype.ASBigRecord;
import edu.jhu.cs639.datatype.ASCompRecord;
import edu.jhu.cs639.util.AssemblyUtils;
import edu.jhu.cs639.util.Constants;
import org.apache.hadoop.fs.Path;
import parquet.avro.AvroParquetReader;
import parquet.avro.AvroParquetWriter;
import parquet.hadoop.ParquetWriter;
import parquet.hadoop.metadata.CompressionCodecName;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This is our driver class for decompression using ASGC format.
 */
public class LocalModeDecompressor {

    private static String previousRegion = "";
    private static int previousAdjustment = 0; // This is to be used while decompressing data.
    private static final Map<String, char[]> assembly = new HashMap<String, char[]>();

    /**
     * This is the entry point for the code. This takes three parameters : <assembly-file> <input-file-prefix> <output-path>
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Please provide three arguments : <assembly-file> <input-file-prefix> <output-path>");
            System.exit(1);
        }
        final String assemblyFilePath = args[0].trim(); // This is the path to the reference genome assembly.
        final String inputFilePrefix = args[1].trim(); // This is the suffix to the files we wrote for aligned and unaligned reads.
        final String outputFilePath = args[2].trim(); // This is the output path where we will write uncompressed data.

        System.out.println("Reading assembly file from : " + assemblyFilePath);
        AssemblyUtils.readAssembly(assemblyFilePath, assembly);
        System.out.println("Finished reading assembly file.");
        decompressFiles(inputFilePrefix, outputFilePath);
    }

    private static void decompressFiles(final String inputFilePrefix, final String outputFilePath) throws IOException {
        final Path outputFile = new Path(new File(outputFilePath).getPath());
        final AvroParquetWriter<ASBigRecord> writer =
                new AvroParquetWriter<ASBigRecord>(outputFile, ASBigRecord.getClassSchema(), CompressionCodecName.UNCOMPRESSED,
                        ParquetWriter.DEFAULT_BLOCK_SIZE, ParquetWriter.DEFAULT_PAGE_SIZE);

        // Approach to decompress aligned records and unaligned records is different.

        final File alignedDataFile = new File(inputFilePrefix + Constants.SUFFIX_ALIGNED);
        if (alignedDataFile.exists()) { // Decompress aligned records if exist.
            long recordRead = 0;
            final AvroParquetReader<ASCompRecord> reader = new AvroParquetReader<ASCompRecord>(new Path(alignedDataFile.getPath()));

            ASCompRecord record;
            while ((record = reader.read()) != null) {
                final ASBigRecord decompressedRecord = decompress(record);
                writer.write(decompressedRecord);
                if (++recordRead % 100000 == 0) {
                    System.out.println("Aligned records read : " + recordRead);
                }
            }
            System.out.println("Total aligned records read  : " + recordRead);
        }

        final File unalignedDataFile = new File(inputFilePrefix + Constants.SUFFIX_UNALIGNED);
        if (unalignedDataFile.exists()) { // Decompress unaligned records if exist.
            long recordRead = 0;
            final AvroParquetReader<ASBigRecord> reader = new AvroParquetReader<ASBigRecord>(new Path(unalignedDataFile.getPath()));

            ASBigRecord record;
            while ((record = reader.read()) != null) {
                record.setQual(ASRecordDecompressor.decompressQuality(record.getQual()));
                writer.write(record);
                if (++recordRead % 100000 == 0) {
                    System.out.println("Unaligned records read : " + recordRead);
                }
            }
            System.out.println("Total unaligned records read : " + recordRead);
        }
        writer.close();
    }

    private static ASBigRecord decompress(final ASCompRecord compressedRecord) {
        final String region = compressedRecord.getRegion();
        if (!region.equalsIgnoreCase(previousRegion)) {
            previousAdjustment = 0;
            previousRegion = region;
        }
        final int trueAlignmentStart = compressedRecord.getStart() + previousAdjustment;
        previousAdjustment = trueAlignmentStart;
        final int referenceLength = compressedRecord.getReferenceLength();
        final char[] referenceString = Arrays.copyOfRange(assembly.get(region), trueAlignmentStart - 1, trueAlignmentStart - 1 + referenceLength);
        return ASRecordDecompressor.decompress(compressedRecord, referenceString);
    }
}
