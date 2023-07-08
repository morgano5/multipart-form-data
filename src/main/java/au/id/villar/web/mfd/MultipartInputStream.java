/*
 * Copyright 2023 Rafael Villar Villar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.id.villar.web.mfd;

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
