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

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.Base64
import org.jboss.netty.channel.local.LocalAddress

import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.mbte.gretty.httpclient.HttpRequestHelper

@Typed class Base64Test extends GroovyTestCase implements HttpRequestHelper {
    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        rest("/data/:mapId/:objectId") {
                            get {
                                def head = request.getHeader(HttpHeaders.Names.AUTHORIZATION)
                                head = head.substring(6)
                                def decoded = Base64.decode(ChannelBuffers.wrappedBuffer(head.bytes))
                                def decodedLine = new String(decoded.array(), decoded.arrayOffset(), decoded.readableBytes())
                                println decodedLine
                                def of = decodedLine.indexOf(':')
                                response.addHeader("User", decodedLine[0..<of])
                                response.addHeader("Password",  decodedLine.substring(of+1))
                            }
                        }
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testMe () {
        GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/data/abracadabra/245"]
        req.setAuthorization ('Alladin', 'open sesame')
        doTest(req) { response ->
            assertEquals "Alladin", response.getHeader("User")
            assertEquals "open sesame", response.getHeader("Password")
        }

    }
}
