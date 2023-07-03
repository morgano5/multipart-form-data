package au.com.villar.web.mfd;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

class MultipartInputStream extends InputStream {

    private final PushbackInputStream wrappedInputStream;
    private final int[] delimiter;

    private int posSuspectedBoundary;
    private int posReturnFakePositive;

    MultipartInputStream(InputStream wrappedInputStream, int[] delimiter) {
        this.wrappedInputStream = new PushbackInputStream(wrappedInputStream);
        this.delimiter = delimiter;
    }

    @Override
    public int read() throws IOException {
        int read = -1;
        if (posReturnFakePositive > 0) {
            read = delimiter[posReturnFakePositive++];
            if (posReturnFakePositive == posSuspectedBoundary) {
                posReturnFakePositive = 0;
                posSuspectedBoundary = 0;
            }
            return read;
        }

        while (posSuspectedBoundary < delimiter.length
                && (read = wrappedInputStream.read()) != -1
                && read == delimiter[posSuspectedBoundary]) {
            posSuspectedBoundary++;
        }

        if (posSuspectedBoundary == delimiter.length) {
            return -1;
        }

        if (posSuspectedBoundary > 0) {
            wrappedInputStream.unread(read);
            return delimiter[posReturnFakePositive++];
        }

        if (read == -1) {
            throw new IOException("Unexpected end of stream reading multipart");
        }

        return read;
    }

}
