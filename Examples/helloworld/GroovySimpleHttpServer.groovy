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

package examples

import org.mbte.gretty.httpserver.GrettyServer

def server = new GrettyServer().
    localAddress(new InetSocketAddress(8080)).
    dir("web").
    defaultHandler {
        response.redirect "/"
    }.
    "/" {
        get {
            response.html = template("web/templates/main.gpptl", [title:'Hello, World!', message: 'Hello, Dynamic World!'])
        }
    }.
    webContext("/myapp", "myapp")

server.start ()

server.doTest("/nosuchurl") { response ->
    assert response.status.code == 301
    assert response.getHeader("Location") == "/"
}

server.doTest("/") { response ->
    assert response.status.code == 200
    def respBody = response.contentText
    assert respBody.contains("<title>Hello, World!</title>")
    assert respBody.contains("Hello, Dynamic World!(from template)<br>")
    assert respBody.contains("<center>HEADER</center>")
    assert respBody.contains("<center>FOOTER</center>")
}

server.stop ()