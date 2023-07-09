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

import java.io.IOException;

/**
 * Listener used to process a part in a multipart request body.
 */
@FunctionalInterface
public interface MultipartProcessorListener {

    /**
     * Called when a part has been detected when parsing a multipart http request body.
     * @param part An object representing the part.
     * @throws IOException If the underlying part throws this exception, or at implementer's discretion.
     */
    void onPart(Part part) throws IOException;

}
