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



package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.handler.codec.http.HttpMethod
import org.mbte.gretty.httpclient.HttpRequestHelper

@Typed class ServerTest extends GroovyTestCase implements HttpRequestHelper {

    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        get("/data/:mapId/set/:objectId") {
                            response.addHeader("mapId", request.parameters.mapId)
                            response.addHeader("objectId",  request.parameters.objectId)
                        }

                        get("/data/") {}

                        post("/post") {
                            response.json = request.contentText.toUpperCase ()
                        }
                    },

                    default: {
                        response.addHeader "Default", "true"
                        for(p in request.parameters.entrySet())
                            response.addHeader(p.key, p.value)
                        response.text = "default: path: ${request.path}"
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testDefault () {
        doTest("/data?msg=12&value=33") { response ->
            def bytes = new byte [response.content.readableBytes()]
            response.content.getBytes(0, bytes)
            def text = new String(bytes, "UTF-8")

            assertEquals "default: path: /data", text
            assertEquals "true", response.getHeader("Default")
            assertEquals "12", response.getHeader("msg")
            assertEquals "33", response.getHeader("value")
        }
    }

    void testMatch() {
        doTest("/data/abracadabra/set/245") { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
        }
    }

    void testNoMatch() {
        doTest("/data") { response ->
            assertNull response.getHeader("mapId")
            assertNull response.getHeader("objectId")
        }
    }

    void testPost() {
        GrettyHttpRequest req = [method:HttpMethod.POST, uri:"/post", json:"{\"obj\":\"mama\"}"]
        doTest(req) { GrettyHttpResponse response ->
            assert response.contentText == "{\"OBJ\":\"MAMA\"}"
        }
    }
}
