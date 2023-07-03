package au.com.villar.web.mfd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultipartHeaders {

    private final Map<String, Object> headerValues = new HashMap<>(2);

    public String getValue(String headerName) {
        Object value = headerValues.get(headerName);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String)value;
        }
        // Only [value instanceof List] is possible from here
        return ((List<?>)value).size() > 0 ? (String)((List<?>) value).get(0) : null;
    }

    public List<String> getValues(String headerName) {
        Object value = headerValues.get(headerName);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return List.of((String)value);
        }
        // Only [value instanceof List] is possible from here
        //noinspection unchecked
        return Collections.unmodifiableList((List<String>) value);
    }

    public Set<String> getHeaders() {
        return headerValues.keySet();
    }

    public static MultipartHeaders parseHeaders(InputStream inputStream) throws IOException {
        MultipartHeaders headers = new MultipartHeaders();
        String headerName;
        do {
            headerName = readHeaderName(inputStream);
            if (!headerName.isEmpty()) {
                String value = readRestOfLine(inputStream);
                headers.addValue(headerName, value);
            }
        } while (!headerName.isEmpty());
        return headers;
    }

    private void addValue(String headerName, String value) {
        Object oldValue = headerValues.get(headerName);
        if (oldValue == null) {
            headerValues.put(headerName, value);
            return;
        }
        if (oldValue instanceof String) {
            List<String> values = new ArrayList<>();
            values.add((String)oldValue);
            values.add(value);
            headerValues.put(headerName, values);
            return;
        }
        // Only [oldValue instanceof List] is possible from here
        //noinspection unchecked
        ((List<String>)oldValue).add(value);
    }

    private static String readHeaderName(InputStream inputStream) throws IOException {
        return readToDelimiter(inputStream, ':');
    }

    private static String readRestOfLine(InputStream inputStream) throws IOException {
        String value = readToDelimiter(inputStream, '\r');
        int read = inputStream.read();
        if (read != '\n') {
            throw new IOException("Unexpected character reading value");
        }
        return value;
    }

    private static String readToDelimiter(InputStream inputStream, char delimiter) throws IOException {
        int read;
        do {
            read = inputStream.read();
        } while (read == ' ');
        if (read == -1) {
            throw new IOException("Unexpected end of stream");
        }
        if (read == '\r') {
            read = inputStream.read();
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            if (read != '\n') {
                throw new IOException("Unexpected character trying to read multipart header");
            }
            return "";
        }
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append((char)read);
        while ((read = inputStream.read()) != delimiter) {
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            headerBuilder.append((char)read);
        }
        return headerBuilder.toString();
    }
}
