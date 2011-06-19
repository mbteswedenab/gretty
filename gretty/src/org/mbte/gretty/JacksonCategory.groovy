/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.gretty

import org.codehaus.jackson.map.ObjectMapper

@Typed class JacksonCategory {
    static final ObjectMapper mapper = []

    static String toJsonString(Object self) {
        mapper.writeValueAsString(self)
    }

    static byte [] toJsonBytes(Object self) {
        mapper.writeValueAsBytes(self)
    }

    static <T> T fromJson(Class<T> self, String json) {
        mapper.readValue(json, self)
    }

    static <T> T fromJson(Class<T> self, byte [] bytes) {
        mapper.readValue(bytes, 0, bytes.length, self)
    }

    static <T> T fromJson(Class<T> self, byte [] bytes, int offset, int length) {
        mapper.readValue(bytes, offset, length, self)
    }
}
