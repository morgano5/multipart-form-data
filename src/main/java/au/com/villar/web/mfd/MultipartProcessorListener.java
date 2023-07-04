package au.com.villar.web.mfd;

import java.io.InputStream;

public interface MultipartProcessorListener {

    void onPart(MultipartHeaders headers, InputStream stream);

}
