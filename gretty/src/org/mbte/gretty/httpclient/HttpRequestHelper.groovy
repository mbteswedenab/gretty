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

package org.mbte.gretty.httpclient

import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.GrettyClient
import groovypp.concurrent.BindLater
import org.jboss.netty.handler.codec.http.HttpResponse
import org.mbte.gretty.httpserver.GrettyHttpRequest
import org.mbte.gretty.httpserver.GrettyHttpResponse

@Trait abstract class HttpRequestHelper {

    void doTest (String request, Function1<GrettyHttpResponse,Void> action) {
        doTest([uri:request], "test_server", action)
    }

    void doTest (String request, String address, Function1<GrettyHttpResponse,Void> action) {
        doTest([uri:request], address, action)
    }

    void doTest (GrettyHttpRequest request, Function1<GrettyHttpResponse,Void> action) {
        doTest(request, "test_server", action)
    }

    void doTest (GrettyHttpRequest request, String address, Function1<GrettyHttpResponse,Void> action) {
        BindLater cdl = []

        GrettyClient client = [new LocalAddress(address)]
        client.connect{ future ->
            client.request(request) { bound ->
                try {
                    action(bound.get())
                    cdl.set("")
                }
                catch(Throwable e) {
                    cdl.setException(e)
                }
            }
        }

        cdl.get()
        client.disconnect ()
    }

    void doTest (String request, String address = "test_server", Closure action) {
        doTest(request, address, { req -> action(req) } )
    }

    void doTest (GrettyHttpRequest request, String address = "test_server", Closure action) {
        doTest(request, address, { req -> action(req) } )
    }
}
