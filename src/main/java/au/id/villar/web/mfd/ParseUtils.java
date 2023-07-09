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

/**
 * Utils to parse content from Strings of InputStreams.
 */
final class ParseUtils {

    private ParseUtils() throws IllegalAccessException {
        throw new IllegalAccessException("No instances for you");
    }

    static String readHeaderName(InputStream inputStream) throws IOException {
        return readToDelimiter(inputStream, ':', true);
    }

    static String readRestOfLine(InputStream inputStream) throws IOException {
        String value = readToDelimiter(inputStream, '\r', false);
        int read = inputStream.read();
        if (read != '\n') {
            throw new IOException("Unexpected character reading value");
        }
        return value;
    }

    static String getValueForKey(String key, String headerValue) {

        final int SCAN_FOR_KEY = 0,     SKIP_KEY = 1,         SKIP_VALUE = 2,       SKIP_TO_QUOTE = 3,
                  SKIP_TO_SPACE = 4,    VALUE_START = 5,      READ_VALUE = 6,       READ_TO_QUOTE = 7,
                  READ_TO_SPACE = 8;

        int state = SKIP_TO_SPACE;
        int valueStart = -1;

        for (int x = 0; x < headerValue.length(); x++) {
            switch (state) {

                case SCAN_FOR_KEY -> {
                    int ch = headerValue.charAt(x);
                    if (indexOfIgnoreCase(headerValue, key, x) == x) {
                        x += key.length() - 1;
                        state = VALUE_START;
                    } else if (ch != ' ' && ch != '\t') {
                        state = SKIP_KEY;
                    }
                }

                case SKIP_KEY -> {
                    if (headerValue.charAt(x) == '=') {
                        state = SKIP_VALUE;
                    }
                }

                case SKIP_VALUE -> state = headerValue.charAt(x) == '"' ? SKIP_TO_QUOTE : SKIP_TO_SPACE;

                case SKIP_TO_QUOTE -> {
                    if (headerValue.charAt(x) == '"') {
                        state = SKIP_TO_SPACE;
                    }
                }

                case SKIP_TO_SPACE -> {
                    if (headerValue.charAt(x) == ' ') {
                        state = SCAN_FOR_KEY;
                    }
                }

                case VALUE_START -> state = headerValue.charAt(x) == '=' ? READ_VALUE : SCAN_FOR_KEY;

                case READ_VALUE -> {
                    state = headerValue.charAt(x) == '"' ? READ_TO_QUOTE : READ_TO_SPACE;
                    valueStart = state == READ_TO_QUOTE ? x + 1 : x;
                }

                case READ_TO_QUOTE -> {
                    if (headerValue.charAt(x) == '"') {
                        return unescapeValue(headerValue, valueStart, x);
                    }
                }

                case READ_TO_SPACE -> {
                    if (headerValue.charAt(x) == ' ') {
                        return unescapeValue(headerValue, valueStart, x);
                    }
                }
            }
        }
        return state == READ_TO_SPACE ? unescapeValue(headerValue, valueStart, headerValue.length()) : null;
    }

    private static String unescapeValue(String headerValue, int start, int end) {
        String rawValue = headerValue.substring(start, end);
        int percentagePos = rawValue.indexOf('%');
        if (percentagePos == -1) {
            return rawValue;
        }

        StringBuilder builder = new StringBuilder(rawValue);
        builder.ensureCapacity(rawValue.length() + 6); // 6 is just an educated guess here
        percentagePos--;

        while ((percentagePos = builder.indexOf("%", percentagePos + 1)) != -1
                && percentagePos + 2 < builder.length()) {
            int ch1 = hexToDecimal(builder.charAt(percentagePos + 1));
            int ch2 = hexToDecimal(builder.charAt(percentagePos + 2));
            if (ch1 != -1 && ch2 != -1) {
                char actualChar = (char)(ch1 * 0x10 + ch2);
                builder.setCharAt(percentagePos, actualChar);
                builder.delete(percentagePos + 1, percentagePos + 3);
            }
            percentagePos++;
        }
        return builder.toString();
    }

    private static int hexToDecimal(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch & 0xF;
        }
        ch |= 0x20;
        if (ch >= 'a' && ch <= 'z') {
            return ch - 87;
        }
        return -1;
    }

    private static String readToDelimiter(InputStream inputStream, char delimiter, boolean toLowerCase)
            throws IOException {
        int lowerBit = toLowerCase ? 0x20 : 0;
        int read;
        do {
            read = readExpected(inputStream);
        } while (read == ' ');
        if (read == '\r') {
            read = readExpected(inputStream);
            if (read != '\n') {
                throw new IOException("Unexpected character trying to read multipart header");
            }
            return "";
        }
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append((char)(read | lowerBit));
        while ((read = readExpected(inputStream)) != delimiter) {
            headerBuilder.append((char)(read | lowerBit));
        }
        return headerBuilder.toString();
    }

    private static int indexOfIgnoreCase(String string, String substring, int fromIndex) {

        // yes, I already know about  string.toLowerCase().indexOf(substring.toLowerCase())

        int limit = string.length() - substring.length();
        for (int x = fromIndex; x <= limit; x++) {
            int y = 0;
            while (y < substring.length() && (substring.charAt(y) | 0x20) == (string.charAt(y + x) | 0x20)) {
                y++;
            }
            if (y == substring.length()) {
                return x;
            }
        }
        return -1;
    }

    private static int readExpected(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        if (read == -1) {
            throw new IOException("Unexpected end of stream");
        }
        return read;
    }



}
