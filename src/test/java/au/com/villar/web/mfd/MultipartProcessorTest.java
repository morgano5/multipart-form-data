package au.com.villar.web.mfd;

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

        List<Part> parts = runTestCase(boundary, content);

        assertEquals(1, parts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", parts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), parts.get(0).headers.getHeaders());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                parts.get(0).headers.getValue("Content-Disposition"));
        assertEquals("text/plain", parts.get(0).headers.getValue("Content-Type"));
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

        List<Part> parts = runTestCase(boundary, content);

        assertEquals(2, parts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", parts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), parts.get(0).headers.getHeaders());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                parts.get(0).headers.getValue("Content-Disposition"));
        assertEquals("text/plain", parts.get(0).headers.getValue("Content-Type"));
        assertEquals("MORE HERE\nLINE 2\nLINE 3\n", parts.get(1).content);
        assertEquals(Set.of("content-disposition", "content-type"), parts.get(1).headers.getHeaders());
        assertEquals("form-data; name=\"file2\"; filename=\"my_file_2.txt\"",
                parts.get(1).headers.getValue("Content-Disposition"));
        assertEquals("text/other", parts.get(1).headers.getValue("Content-Type"));
    }

    @Test
    void shouldIgnoreContentBeforeFirstDelimiter() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "Some random text before any content--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--\r\n";

        List<Part> parts = runTestCase(boundary, content);

        assertEquals(1, parts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", parts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), parts.get(0).headers.getHeaders());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                parts.get(0).headers.getValue("Content-Disposition"));
        assertEquals("text/plain", parts.get(0).headers.getValue("Content-Type"));
    }

    @Test
    void shouldIgnoreContentAfterLastDelimiter() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--Some random content after the multipart section\r\n";

        List<Part> parts = runTestCase(boundary, content);

        assertEquals(1, parts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", parts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), parts.get(0).headers.getHeaders());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                parts.get(0).headers.getValue("Content-Disposition"));
        assertEquals("text/plain", parts.get(0).headers.getValue("Content-Type"));
    }

    @Test
    void shouldParseEmptyParts() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"my_file.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n\r\n"
                + "--" + boundary + "--\r\n";

        List<Part> parts = runTestCase(boundary, content);

        assertEquals(1, parts.size());
        assertEquals("", parts.get(0).content);
        assertEquals(Set.of("content-disposition", "content-type"), parts.get(0).headers.getHeaders());
        assertEquals("form-data; name=\"file1\"; filename=\"my_file.txt\"",
                parts.get(0).headers.getValue("Content-Disposition"));
        assertEquals("text/plain", parts.get(0).headers.getValue("Content-Type"));
    }

    @Test
    void shouldParsePartsWithNoHeaders() throws IOException {
        String boundary = "---------------------------40484630840702506701393865460";
        String content = "--" + boundary + "\r\n\r\nCONTENT HERE\nLINE 2\nLINE 3\n\r\n"
                + "--" + boundary + "--\r\n";

        List<Part> parts = runTestCase(boundary, content);

        assertEquals(1, parts.size());
        assertEquals("CONTENT HERE\nLINE 2\nLINE 3\n", parts.get(0).content);
        assertEquals(Set.of(), parts.get(0).headers.getHeaders());
    }

    private static List<Part> runTestCase(String boundary, String content) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());

        List<Part> parts = new ArrayList<>();
        MultipartProcessor.process(boundary, inputStream, ((headers, stream) -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                stream.transferTo(buffer);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            parts.add(new Part(headers, buffer.toString()));
        }));
        return parts;
    }

    private record Part(MultipartHeaders headers, String content) {}
}