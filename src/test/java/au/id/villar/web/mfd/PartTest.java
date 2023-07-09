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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PartTest {

    private static final int[] EMPTY = new int[0];

    @Test
    void shouldReadSingleHeader() throws IOException {
        Part part = Part.readPart(toStream("header1: value1\r\n\r\n"), EMPTY);

        assertEquals(Set.of("header1"), part.getHeaderNames());
        assertEquals("value1", part.getHeaderValue("header1"));
    }

    @Test
    void shouldReadMultiHeader() throws IOException {
        Part part = Part.readPart(toStream("header1: value1\r\nheader2: value2\r\n\r\n"), EMPTY);

        assertEquals(Set.of("header1", "header2"), part.getHeaderNames());
        assertEquals("value1", part.getHeaderValue("header1"));
        assertEquals("value2", part.getHeaderValue("header2"));
    }

    @Test
    void shouldReadMultipleValuesSameHeader() throws IOException {
        Part part = Part.readPart(toStream("header1: value1\r\nheader1: value2\r\n\r\n"), EMPTY);

        assertEquals(Set.of("header1"), part.getHeaderNames());
        assertEquals("value1", part.getHeaderValue("header1"));
        assertEquals(2, part.getHeaderValues("header1").size());
        assertEquals(Set.of("value1", "value2"), new HashSet<>(part.getHeaderValues("header1")));
    }

    @Test
    void shouldNotReadBeyondHeaderSection() throws IOException {
        InputStream stream = toStream("header1: value1\r\nheader2: value2\r\n\r\nXXX");
        Part.readPart(stream, EMPTY);
        int ch1 = stream.read();
        int ch2 = stream.read();
        int ch3 = stream.read();
        int ch4 = stream.read();

        assertEquals('X', ch1);
        assertEquals('X', ch2);
        assertEquals('X', ch3);
        assertEquals(-1, ch4);
    }

    @Test
    void shouldExtractFileAndFilename() throws IOException {
        Part part = Part.readPart(
                toStream("Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n\r\n"), EMPTY);

        assertEquals("file1", part.getName());
        assertEquals("my_file.txt", part.getFilename());
    }

    @Test
    void shouldUnescapeFieldValues() throws IOException {
        Part part = Part.readPart(
                toStream("Content-Disposition: form-data; name=\"file1\"; filename=\"my%20file.txt\"\r\n\r\n"), EMPTY);

        assertEquals("file1", part.getName());
        assertEquals("my file.txt", part.getFilename());
    }

    @Test
    void shouldUnescapeWeirdFieldValues() throws IOException {
        Part part = Part.readPart(
                toStream("Content-Disposition: form-data; name=\"file1\"; filename=\"my%20file%0A.txt\"\r\n\r\n"),
                EMPTY);

        assertEquals("file1", part.getName());
        assertEquals("my file\n.txt", part.getFilename());
    }


    @Test
    void shouldRetrieveBodyAsString() throws IOException {
        Part part = Part.readPart(
                toStream("Content-Disposition: form-data; name=\"field1\"\r\n\r\nTESTING_DATA\r\n----XX"),
                new int[] {'\r', '\n', '-', '-', '-', '-', 'X', 'X'});

        assertEquals("TESTING_DATA", part.readBodyText());
    }


    @Test
    void shouldRetrieveBodyAsStringWithCharset() throws IOException {
        Part part = Part.readPart(
                toStream("Content-Disposition: form-data; name=\"field1\"\r\n\r\nTESTING_DATA\r\n----XX"),
                new int[] {'\r', '\n', '-', '-', '-', '-', 'X', 'X'});

        assertEquals("TESTING_DATA", part.readBodyText(Charset.defaultCharset()));
    }

    private InputStream toStream(String header) {
        return new ByteArrayInputStream(header.getBytes());
    }
}