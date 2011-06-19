/*
 * Copyright 2009-2011 MBTE Sweden AB.
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
import org.mbte.gretty.test.GrettyServerTestCase

@Typed class CookiesTest extends GrettyServerTestCase {
    protected void buildServer() {
        server = [
            default: {
                def cookie = request.getCookie("JSESSIONID")
                if (cookie) {
                    response.text = cookie.value
                }
                else {
                    response.text = "no session cookie"
                    response.addCookie("JSESSIONID", UUID.randomUUID().toString())
                }
            }
        ]
    }

    void testMe () {
        GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/template/mama"]
        Reference ref = []
        doTest(req) { GrettyHttpResponse response ->
            ref = response.getCookie("JSESSIONID").value
            assert response.contentText == "no session cookie"
        }

        req = [method:HttpMethod.GET, uri:"/template/mama"]
        req.addCookie("JSESSIONID", ref.get().toString())
        doTest(req) { GrettyHttpResponse response ->
            assert response.contentText == ref.get()
        }
    }
}
