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

package org.mbte.gretty.httpserver

import freemarker.template.Configuration
import freemarker.template.ObjectWrapper
import org.codehaus.jackson.map.ObjectMapper

@Typed abstract class GrettyHttpHandler implements Cloneable {
    private static final ObjectMapper mapper = []

    GrettyHttpRequest  request
    GrettyHttpResponse response
    GrettyServer       server

    final void handle (GrettyHttpRequest request, GrettyHttpResponse response, Map<String,String> pathArgs) {
        GrettyHttpHandler clone = clone ()
        clone.request  = request
        clone.response = response
        clone.handle (pathArgs)
    }

    final void redirect(String where) {
        response.redirect(where)
    }

    final String template(String file, Map root, Function1<Map,Void> dataBinding) {
        root.request = request
        if (dataBinding) {
            dataBinding(root)
        }

        def template = Configuration.getDefaultConfiguration().getTemplate(file)
        def writer = new StringWriter()
        template.process (root, writer, ObjectWrapper.BEANS_WRAPPER)
        writer.toString()
    }

    final String template(String file, Map root = [:]) {
        template(file, root, null)
    }

    final String template(String file, Function1<Map,Void> dataBinding) {
        template(file, [:], dataBinding)
    }

    static <T> T fromJson(Class<T> clazz, String jsonText) {
        try {
            mapper.readValue(jsonText, clazz)
        }
        catch(t) {
            return null
        }
    }

    static <T> String toJson(T object) {
        def writer = new StringWriter()
        mapper.writeValue(writer, object)
        writer.toString()
    }

    abstract void handle(Map<String,String> pathArguments)
}
