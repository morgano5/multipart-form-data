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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MultipartHeadersTest {

    @Test
    void shouldReadSingleHeader() throws IOException {
        MultipartHeaders headers = MultipartHeaders.parseHeaders(toStream("header1: value1\r\n\r\n"));

        assertEquals(Set.of("header1"), headers.getHeaders());
        assertEquals("value1", headers.getValue("header1"));
    }

    @Test
    void shouldReadMultiHeader() throws IOException {
        MultipartHeaders headers = MultipartHeaders.parseHeaders(
                toStream("header1: value1\r\nheader2: value2\r\n\r\n"));

        assertEquals(Set.of("header1", "header2"), headers.getHeaders());
        assertEquals("value1", headers.getValue("header1"));
        assertEquals("value2", headers.getValue("header2"));
    }

    @Test
    void shouldReadMultipleValuesSameHeader() throws IOException {
        MultipartHeaders headers = MultipartHeaders.parseHeaders(
                toStream("header1: value1\r\nheader1: value2\r\n\r\n"));

        assertEquals(Set.of("header1"), headers.getHeaders());
        assertEquals("value1", headers.getValue("header1"));
        assertEquals(2, headers.getValues("header1").size());
        assertEquals(Set.of("value1", "value2"), new HashSet<>(headers.getValues("header1")));
    }

    @Test
    void shouldNotReadBeyondHeaderSection() throws IOException {
        InputStream stream = toStream("header1: value1\r\nheader2: value2\r\n\r\nXXX");
        MultipartHeaders.parseHeaders(stream);
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
        MultipartHeaders headers = MultipartHeaders.parseHeaders(
                toStream("Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n\r\n"));

        assertEquals("file1", headers.getName());
        assertEquals("my_file.txt", headers.getFilename());
    }

    @Test
    void shouldUnescapeFieldValues() throws IOException {
        MultipartHeaders headers = MultipartHeaders.parseHeaders(
                toStream("Content-Disposition: form-data; name=\"file1\"; filename=\"my%20file.txt\"\r\n\r\n"));

        assertEquals("file1", headers.getName());
        assertEquals("my file.txt", headers.getFilename());
    }

    @Test
    void shouldUnescapeWeirdFieldValues() throws IOException {
        MultipartHeaders headers = MultipartHeaders.parseHeaders(
                toStream("Content-Disposition: form-data; name=\"file1\"; filename=\"my%20file%0A.txt\"\r\n\r\n"));

        assertEquals("file1", headers.getName());
        assertEquals("my file\n.txt", headers.getFilename());
    }

    private InputStream toStream(String header) {
        return new ByteArrayInputStream(header.getBytes());
    }
}