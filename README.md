# multipart-form-data

A brief, framework-agnostic, pseudo-reactive style library to parse the body of a multipart/form-data request.


## How to use

You need to somehow get two objects from a given HTTP request containing a multipart/form-data body:

- The "boundary" header (`java.lang.String`)
- A `java.io.InputStream` representing the stream of the request's body

Where the boundary is the main part of the delimiter used to separate different parts in the multipart body, as
specified in [RFC-7578](https://datatracker.ietf.org/doc/html/rfc7578) and
[RFC-2046, section-5.1](https://datatracker.ietf.org/doc/html/rfc2046#section-5.1).

### 1. Create your listener:

```java
public class MyListener implements MultipartProcessorListener {

    Path storageDir = Path.of("/path/to/dir");

    @Override
    public void onPart(MultipartHeaders headers, InputStream stream) throws IOException {

        String filename = headers.getFilename();
        Path destinationPath = storageDir.resolve(filename);

        try (FileOutputStream destination = new FileOutputStream(destinationPath.toFile())) {
            stream.transferTo(destination);
        }
    }

}
```

### 2. Consume the stream:

```java

String boundary = request.getBoundary();
InputStream body = request.getBody();

MyListener listener = new MyListener();

MultipartProcessor.process(boundary, body, listener);
```
<hr>
Copyright &copy;2023 Rafael Villar Villar. All rights reserved.