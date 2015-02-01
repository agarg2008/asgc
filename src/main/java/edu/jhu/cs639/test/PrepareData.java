package edu.jhu.cs639.test;

import java.io.*;

public class PrepareData {

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("test-files/sample_fastq"));
        BufferedWriter bw = new BufferedWriter(new FileWriter("test-files/non-scalce_read"));
        int count = 0;
        String line;
        while ((line = br.readLine()) != null) {
            if (++count % 4 == 2) {
                bw.write(line);
                bw.newLine();
            }

        }
        br.close();
        bw.close();
    }
}
