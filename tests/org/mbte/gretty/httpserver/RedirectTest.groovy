/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.gretty.httpserver

import org.mbte.gretty.httpclient.GrettyClient
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion

@Typed class RedirectTest extends GroovyTestCase {
    void testGoogle () {
        GrettyClient client = [new InetSocketAddress("www.google.com", 80)]
        client.connect().await()

        GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, ""]
        req.keepAlive()

        client.request(req, null, true).get()
    }

    void testGoogle2 () {
        GrettyClient client = [new InetSocketAddress("www.google.com", 80)]
        client.connect().await()

        GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, "/"]
        req.keepAlive()

        client.request(req, null, true).get()
    }

    void testMicrosoft () {
        GrettyClient client = [new InetSocketAddress("www.microsoft.com", 80)]
        client.connect().await()

        GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, "/"]
        req.keepAlive()

        client.request(req, null, true).get()
    }
}
