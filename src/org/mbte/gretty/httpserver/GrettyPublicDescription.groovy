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

@Typed abstract class GrettyPublicDescription implements Runnable {
    GrettyContext context

    void options(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.OPTIONS, match, handler)
    }

    void get(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.GET, match, handler)
    }

    void head(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.HEAD, match, handler)
    }

    void post(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.POST, match, handler)
    }

    void put(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.PUT, match, handler)
    }

    void patch(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.PATCH, match, handler)
    }

    void delete(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.DELETE, match, handler)
    }

    void trace(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.TRACE, match, handler)
    }

    void connect(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.CONNECT, match, handler)
    }

    void websocket(String match, GrettyWebSocketHandler handler) {
        context.addWebSocket(match, handler)
    }

    void rest(String match, GrettyRestDescription descr) {
        descr.context = context
        descr.match = match
        descr.run ()
    }
}
