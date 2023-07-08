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

public final class MultipartProcessor {

    private MultipartProcessor() throws IllegalAccessException {
        throw new IllegalAccessException("No instances for you");
    }

    public static void process(String boundary, InputStream input, MultipartProcessorListener listener)
            throws IOException {

        int[] delimiter = calculateDelimiterBytes(boundary);
        consumeInitialDelimiter(input, delimiter);
        while(!endDetectedConsumingNewLine(input)) {
            MultipartHeaders headers = MultipartHeaders.parseHeaders(input);
            InputStream partBody = new MultipartInputStream(input, delimiter);
            listener.onPart(headers, partBody);
        }
    }

    private static boolean endDetectedConsumingNewLine(InputStream input) throws IOException {
        int ch1 = input.read();
        int ch2 = input.read();
        if (ch1 == '\r' && ch2 == '\n') {
            return false;
        }
        if (ch1 == '-' && ch2 == '-') {
            while (ch1 != -1) {
                ch1 = input.read();
            }
            return true;
        }
        throw new IOException("Unexpected char sequence reading between parts");
    }

    private static int[] calculateDelimiterBytes(String boundary) {
        byte[] boundaryBytes = boundary.getBytes();
        int[] delimiter = new int[boundaryBytes.length + 4];

        for(int x = 0; x < boundaryBytes.length; x++) {
            delimiter[x + 4] = 0xFF & boundaryBytes[x];
        }
        delimiter[0] = '\r';
        delimiter[1] = '\n';
        delimiter[2] = '-';
        delimiter[3] = '-';
        return delimiter;
    }

    private static void consumeInitialDelimiter(InputStream input, int[] delimiter) throws IOException {

        int read = -1;
        int suspectedDelimiterRead = 2;
        while (suspectedDelimiterRead < delimiter.length && (read = input.read()) != -1) {
            if (read == delimiter[suspectedDelimiterRead]) {
                suspectedDelimiterRead++;
            } else {
                suspectedDelimiterRead = 2;
            }
        }

        if (read == -1) {
            throw new IOException("Initial delimiter not found");
        }
    }

}
