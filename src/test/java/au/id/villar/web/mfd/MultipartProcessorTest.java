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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MultipartProcessorTest {

    @Test
    void shouldParseSingleMultiPart() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--\r\n";

        List<ResultPart> resultParts = runTestCase(boundary, content);

        assertEquals(1, resultParts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", resultParts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), resultParts.get(0).part.getHeaderNames());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                resultParts.get(0).part.getHeaderValue("Content-Disposition"));
        assertEquals("text/plain", resultParts.get(0).part.getHeaderValue("Content-Type"));
    }

    @Test
    void shouldParseMoreThanOnePart() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file2\"; filename=\"my_file_2.txt\"\r\n"
                + "Content-Type: text/other\r\n\r\nMORE HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--\r\n";

        List<ResultPart> resultParts = runTestCase(boundary, content);

        assertEquals(2, resultParts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", resultParts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), resultParts.get(0).part.getHeaderNames());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                resultParts.get(0).part.getHeaderValue("Content-Disposition"));
        assertEquals("text/plain", resultParts.get(0).part.getHeaderValue("Content-Type"));
        assertEquals("MORE HERE\nLINE 2\nLINE 3\n", resultParts.get(1).content);
        assertEquals(Set.of("content-disposition", "content-type"), resultParts.get(1).part.getHeaderNames());
        assertEquals("form-data; name=\"file2\"; filename=\"my_file_2.txt\"",
                resultParts.get(1).part.getHeaderValue("Content-Disposition"));
        assertEquals("text/other", resultParts.get(1).part.getHeaderValue("Content-Type"));
    }

    @Test
    void shouldIgnoreContentBeforeFirstDelimiter() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "Some random text before any content--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--\r\n";

        List<ResultPart> resultParts = runTestCase(boundary, content);

        assertEquals(1, resultParts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", resultParts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), resultParts.get(0).part.getHeaderNames());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                resultParts.get(0).part.getHeaderValue("Content-Disposition"));
        assertEquals("text/plain", resultParts.get(0).part.getHeaderValue("Content-Type"));
    }

    @Test
    void shouldIgnoreContentAfterLastDelimiter() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--Some random content after the multipart section\r\n";

        List<ResultPart> resultParts = runTestCase(boundary, content);

        assertEquals(1, resultParts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", resultParts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), resultParts.get(0).part.getHeaderNames());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                resultParts.get(0).part.getHeaderValue("Content-Disposition"));
        assertEquals("text/plain", resultParts.get(0).part.getHeaderValue("Content-Type"));
    }

    @Test
    void shouldParseEmptyParts() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n\r\n"
                + "--" + boundary + "--\r\n";

        List<ResultPart> resultParts = runTestCase(boundary, content);

        assertEquals(1, resultParts.size());
        assertEquals("", resultParts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), resultParts.get(0).part.getHeaderNames());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                resultParts.get(0).part.getHeaderValue("Content-Disposition"));
        assertEquals("text/plain", resultParts.get(0).part.getHeaderValue("Content-Type"));
    }

    @Test
    void shouldParsePartsWithNoHeaders() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n--" + boundary + "--\r\n";

        List<ResultPart> resultParts = runTestCase(boundary, content);

        assertEquals(1, resultParts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", resultParts.get(0).content);
        assertEquals(Set.of(), resultParts.get(0).part.getHeaderNames());
    }

    @Test
    void shouldConsumeEachPartBeforeTryingNextOne() throws IOException {
        String boundary = "---ZZZZ";
        String content = "--" + boundary + "\r\nContent-disposition:form-data; name=\"field1\"\r\n\r\n"
                + "LINE 1\nLINE 2\nLINE 3\n\r\n--" + boundary + "\r\nContent-disposition:form-data; name=\"field2\""
                + "\r\n\r\nANOTHER LINE\n\r\n--" + boundary + "--";

        List<ResultPart> onlySecond = new ArrayList<>();
        try (ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes())) {
            MultipartProcessor.process(boundary, input, part -> {
                if ("field1".equals(part.getName())) {
                    return;
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                part.getBodyStream().transferTo(buffer);
                onlySecond.add(new ResultPart(part, buffer.toString()));
            });
        }

        assertEquals(1, onlySecond.size());
        assertEquals("field2", onlySecond.get(0).part().getName());
        assertEquals("ANOTHER LINE\n", onlySecond.get(0).content);
    }

    @Test
    void shouldExtractBoundary() {
        String contentType = "multipart/form-data; boundary=----ABC";
        String boundary = MultipartProcessor.extractBoundary(contentType);

        assertEquals("----ABC", boundary);
    }

    @Test
    void shouldReturnNullIfNotBoundaryProvided() {
        String contentType = "multipart/form-data; noboundary=----ABC";
        String boundary = MultipartProcessor.extractBoundary(contentType);

        assertNull(boundary);
    }

    private static List<ResultPart> runTestCase(String boundary, String content) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());

        List<ResultPart> resultParts = new ArrayList<>();
        MultipartProcessor.process(boundary, inputStream, ((part) -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                part.getBodyStream().transferTo(buffer);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            resultParts.add(new ResultPart(part, buffer.toString()));
        }));
        return resultParts;
    }

    private record ResultPart(Part part, String content) {}
}