package edu.jhu.cs639.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class ignores newLine character in inputStream. Wrote to ignore newLine chars in FASTA file.
 */
public class SkipNewLineInputStream extends FileInputStream {

    public SkipNewLineInputStream(final String file) throws FileNotFoundException {
        super(file);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = 0, c;
        do {
            c = this.read();
            if (c != -1) {
                b[off + n] = (byte) c;
                n++;
                len--;
            }
        } while (c != -1 && len > 0);
        return n;
    }

    @Override
    public int read() throws IOException {
        int c;
        do {
            c = super.read();
        } while (c != -1 && (c == '\n' || c == '\r'));
        return c;
    }
}