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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Object representing a part detected when parsing an HTTP multipart request body.
 */
public class Part {

    private final Map<String, Object> headerValues = new HashMap<>(2);
    private String name;
    private String filename;
    private InputStream input;

    /**
     * Retrieves the first value found for a header.
     * @param headerName the name of the header whose value is requested.
     * @return The first value found for the given header.
     */
    public String getHeaderValue(String headerName) {
        headerName = headerName.toLowerCase();
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

    /**
     * Retrieves all the values found for a given header.
     * @param headerName the name of the header whose values are requested.
     * @return A list containing all values found for the given header.
     */
    @SuppressWarnings("unchecked")
    public List<String> getHeaderValues(String headerName) {
        headerName = headerName.toLowerCase();
        Object value = headerValues.get(headerName);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return List.of((String)value);
        }
        // Only [value instanceof List] is possible from here
        return Collections.unmodifiableList((List<String>) value);
    }

    /**
     * Retrieves all headers found when parsing the part represented by this object.
     * @return A {@link java.util.List} containing all headers found when parsing the part represented by this object.
     */
    public Set<String> getHeaderNames() {
        return headerValues.keySet();
    }

    /**
     * Returns the field "name" from the Content-Disposition header.
     * @return The value of the field "name" from the Content-Disposition header, or {@code null} if it wasn't found.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the field "filename" from the Content-Disposition header.
     * @return The value of the field "filename" from the Content-Disposition header, or {@code null} otherwise.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the stream representing this part's body. This {@link java.io.InputStream} is not reset every time this
     * method is called, it rather returns the same instance every time. This instance may return EOF (-1) if it has
     * been already consumed by one of the convenience methods {@link Part#readBodyText()} or
     * {@link Part#readBodyText(Charset)}.
     * @return the {@link java.io.InputStream} representing this part's body.
     */
    public InputStream getBodyStream() {
        return input;
    }

    /**
     * Convenience method to read this part's body into a single {@link java.lang.String}.
     * @return A {@link java.lang.String} containing this part's body.
     * @throws IOException If the underlying {@link java.io.InputStream} throws this exception.
     */
    public String readBodyText() throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            input.transferTo(buffer);
            return buffer.toString();
        }
    }

    /**
     * Convenience method to read this part's body into a single {@link java.lang.String}.
     * @param charset The {@link java.nio.charset.Charset} used to parse this Part's body into a
     * {@link java.lang.String}
     * @return A {@link java.lang.String} containing this part's body.
     * @throws IOException If the underlying {@link java.io.InputStream} throws this exception.
     */
    public String readBodyText(Charset charset) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            input.transferTo(buffer);
            return buffer.toString(charset);
        }
    }

    static Part readPart(InputStream inputStream, int[] delimiter) throws IOException {
        Part part = new Part();
        String headerName;
        do {
            headerName = ParseUtils.readHeaderName(inputStream);
            if (!headerName.isEmpty()) {
                String value = ParseUtils.readRestOfLine(inputStream);
                part.addValue(headerName, value);
                if ("content-disposition".equals(headerName)) {
                    part.assignNameAndFilename(value);
                }
            }
        } while (!headerName.isEmpty());
        part.input = new MultipartInputStream(inputStream, delimiter);
        return part;
    }

    @SuppressWarnings("unchecked")
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
        ((List<String>)oldValue).add(value);
    }

    private void assignNameAndFilename(String headerValue) {

        if(!headerValue.startsWith("form-data")) {
            return;
        }

        String nameValue = ParseUtils.getValueForKey("name", headerValue);
        String filenameValue = ParseUtils.getValueForKey("filename", headerValue);

        if (nameValue != null) {
            this.name = nameValue;
        }

        if (filenameValue != null) {
            this.filename = filenameValue;
        }
    }

}
