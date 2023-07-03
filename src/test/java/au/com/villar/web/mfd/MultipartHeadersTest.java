package au.com.villar.web.mfd;

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

    private InputStream toStream(String header) {
        return new ByteArrayInputStream(header.getBytes());
    }
}