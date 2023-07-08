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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MultipartInputStreamTest {

    private InputStream createTestCase(String part) {
        InputStream wrappedStream = new ByteArrayInputStream(part.getBytes());
        return createTestCase(wrappedStream);
    }

    private InputStream createTestCase(InputStream wrappedStream) {
        byte[] rawDelimiterBytes = "DELIMITER".getBytes();
        int[] rawDelimiter = new int[rawDelimiterBytes.length + 4];
        for (int x = 0; x < rawDelimiterBytes.length; x++) {
            rawDelimiter[x + 4] = 0xFF & rawDelimiterBytes[x];
        }
        rawDelimiter[0] = '\r';
        rawDelimiter[1] = '\n';
        rawDelimiter[2] = '-';
        rawDelimiter[3] = '-';

        return new MultipartInputStream(wrappedStream, rawDelimiter);
    }

    @Test
    void shouldReadSimpleMultipart() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream testingStream  = createTestCase("Testing content\r\nLine Two\r\n--DELIMITER")) {
            testingStream.transferTo(buffer);
        }
        String finalPart = buffer.toString();

        assertEquals("Testing content\r\nLine Two", finalPart);
    }

    @Test
    void shouldReadContentWithFirstPartOfDelimiterAtTheBeginning() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream testingStream  = createTestCase(
                "--DELIMITzzz\r\nTesting content\r\nLine Three\r\n--DELIMITER")) {
            testingStream.transferTo(buffer);
        }
        String finalPart = buffer.toString();

        assertEquals("--DELIMITzzz\r\nTesting content\r\nLine Three", finalPart);
    }

    @Test
    void shouldReadContentWithFirstPartOfDelimiterInTheMiddle() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream testingStream  = createTestCase(
                "Testing content\r\nLine Two\r\n--DELIMITzzz\r\n--DELIMITER")) {
            testingStream.transferTo(buffer);
        }
        String finalPart = buffer.toString();

        assertEquals("Testing content\r\nLine Two\r\n--DELIMITzzz", finalPart);
    }

    @Test
    void shouldReadContentWithFirstPartOfDelimiterAtTheEnd() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream testingStream  = createTestCase("Testing content\r\nLine Two\r\n--DELIMIT\r\n--DELIMITER")) {
            testingStream.transferTo(buffer);
        }
        String finalPart = buffer.toString();

        assertEquals("Testing content\r\nLine Two\r\n--DELIMIT", finalPart);
    }

    @Test
    void shouldNotConsumeMoreBytesThanMultipartContent() throws IOException {
        InputStream wrappedStream = new ByteArrayInputStream(
                "Testing content\r\nLine Two\r\n--DELIMITERXXX".getBytes());
        InputStream testingStream  = createTestCase(wrappedStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        testingStream.transferTo(buffer);

        int ch1 = wrappedStream.read();
        int ch2 = wrappedStream.read();
        int ch3 = wrappedStream.read();
        int ch4 = wrappedStream.read();

        assertEquals('X', ch1);
        assertEquals('X', ch2);
        assertEquals('X', ch3);
        assertEquals(-1, ch4);
    }

    @Test
    void shouldThrowIoExceptionIfStreamEndsUnexpected() {
        assertThrows(IOException.class, () -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream testingStream  = createTestCase("Testing content\r\nLine Two\r\n--DEL")) {
                testingStream.transferTo(buffer);
            }
        });
    }
}