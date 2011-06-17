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

import org.jboss.netty.handler.codec.http.HttpMethod
import sun.misc.Request

@Typed class TemplateTest extends GrettyServerTestCase {

    File scriptFile, templateFile

    protected void buildServer() {
        File root = ["."]
        root = root.canonicalFile
        templateFile = File.createTempFile("temp_", "_script.gpptl", root)
        templateFile.text = """\
Request path: \${request.uri} URI: \${uri ?: request.uri.toUpperCase() }\
"""
        templateFile.deleteOnExit()

        scriptFile = File.createTempFile("temp_", "_script.groovy", root)
        scriptFile.text = """\
        print "issue: \${request.parameters.issue[0]} reporter: \${request.parameters.reporter[0]}"
"""
        scriptFile.deleteOnExit()

        server = [
            default: {
                response.text = template(templateFile.absolutePath, [:]) { binding ->
                    binding.uri = request.uri.toUpperCase ()
                }
                response.addHeader "Template", "true"
            }
        ]

        // we don't used server.dir property as it will complain on missing directory 'static'
        server.defaultContext.groovletFiles = "."
    }

    void testTemplate() {
        GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/template/lala"]
        doTest(req) { GrettyHttpResponse response ->
            assert response.contentText == "Request path: /template/lala URI: /TEMPLATE/LALA"
            assert response.getHeader("Template") == "true"
        }
    }

    void testTemplateNonDefault() {
        // path to our script
        def path = templateFile.name.substring(0, templateFile.name.length() - 6)

        GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/${path}"]
        doTest(req) { GrettyHttpResponse response ->
            assert response.contentText == "Request path: /$path URI: /${path.toUpperCase()}"
            assert !response.getHeader("Template")
        }
    }

    void testScript() {
        // path to our script
        def path = scriptFile.name.substring(0, scriptFile.name.length() - 7)

        GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/${path}?issue=239&reporter=wilson"]
        doTest(req) { GrettyHttpResponse response ->
            assert response.contentText == "issue: 239 reporter: wilson"
        }
    }
}
