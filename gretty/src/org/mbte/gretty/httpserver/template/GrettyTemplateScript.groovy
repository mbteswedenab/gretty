/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mbte.gretty.httpserver.template

import org.mbte.gretty.httpserver.GrettyHttpRequest
import org.mbte.gretty.httpserver.GrettyHttpResponse
import org.mbte.gretty.httpserver.GrettyHttpHandler
import groovypp.text.GppTemplateScript
import groovypp.text.FastPrintWriter

@Typed abstract class GrettyTemplateScript extends GppTemplateScript{
    GrettyHttpRequest    request
    GrettyHttpResponse   response
    GrettyTemplateEngine templateEngine
    File                 file

    void setParent(GrettyTemplateScript parent) {
        request = parent.request
        response = parent.response
        templateEngine = parent.templateEngine
        out = parent.out
    }

    void setGrettyHttpHandler(GrettyHttpHandler grettyHttpHandler) {
        request  = grettyHttpHandler.request
        response = grettyHttpHandler.response
        templateEngine = grettyHttpHandler.context.templateEngine
    }

    def getUnresolvedProperty(String name) {
        binding.variables[name]
    }

    void setUnresolvedProperty(String name, def value) {
        binding.variables[name] = value
    }

    protected void flush() {
        out.flush()
    }

    protected Function1<Map<String,Object>,Object> loadTemplate(String name) {
        def file = new File(file.parentFile, name).canonicalFile
        def template = templateEngine.getTemplateClass(file)

        def that = this
        return { extra ->
            GrettyTemplateScript script = template.newInstance(new Binding(binding.variables))
            script.parent = that

            if(extra) {
                binding.variables.putAll(extra)
            }

            script.run()
            flush()

            if(extra) {
                for(e in extra.keySet())
                    binding.variables.remove(e)
            }
        }
    }

    protected void include(String name, Map<String,Object> extra = null) {
        loadTemplate(name).call(extra)
    }
}
