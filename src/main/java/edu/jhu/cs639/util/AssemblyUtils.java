package edu.jhu.cs639.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * This is a utility class methods dealing with assembly.
 */
public class AssemblyUtils {

    /**
     * Given the location of assembly file name it fills the assembly map for each region in the assembly.
     */
    public static void readAssembly(final String assemblyFile, final Map<String, char[]> assembly) throws IOException {
        long t = System.currentTimeMillis();
        final BufferedReader br = new BufferedReader(new FileReader(assemblyFile));
        final StringBuilder regionBuilder = new StringBuilder();
        String line;
        String name = null;
        long count = 0;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(">")) {
                if (name != null) {
                    final int length = regionBuilder.length();
                    final char[] region = new char[length];
                    regionBuilder.getChars(0, length, region, 0);
                    assembly.put(name, region);
                }
                name = line.substring(1, line.length());
                regionBuilder.setLength(0);
            } else {
                // In assembly there are certain regions in lowercase to denote some other properties of assembly.
                // We do not need to be concerned about that information thus converting everything to upper-case.
                regionBuilder.append(line.toUpperCase());
            }
            if (++count % 1000000 == 0) {
                System.out.println("Read " + count + " lines.");
            }
        }
        final int length = regionBuilder.length();
        final char[] region = new char[length];
        regionBuilder.getChars(0, length, region, 0);
        assembly.put(name, region);
        System.out.println("Took " + (System.currentTimeMillis() - t) / 1000 + " seconds to load assembly.");
    }
}
