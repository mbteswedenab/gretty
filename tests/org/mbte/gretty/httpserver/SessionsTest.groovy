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

@Typed class SessionsTest extends GrettyServerTestCase {
    protected void buildServer() {
        server = [
            default: {
                Integer obj = session.counter
                if(obj == null) {
                    session.counter = obj = 1
                }
                else {
                    obj = obj + 1
                    session.counter = obj
                }
                response.text = obj
            }
        ]
    }

    void testMe () {
        Reference sessionId = []
        for (i in 0..<10) {
            GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/template/mama"]
            if(sessionId.get())
                req.addCookie("JSESSIONID", sessionId.get().toString())

            doTest(req) { GrettyHttpResponse response ->
                def session = response.getCookie("JSESSIONID")?.value
                sessionId.set(session)
                println "$i ${response.contentText} $session"
                assert response.contentText == "${i+1}"
            }
        }
    }
}
