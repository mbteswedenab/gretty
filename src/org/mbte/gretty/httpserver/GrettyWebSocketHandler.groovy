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

import org.jboss.netty.handler.codec.http.HttpRequest

@Typed abstract class GrettyWebSocketHandler extends GrettyWebSocketListener implements Cloneable {
    protected volatile GrettyWebSocket socket

    protected String socketPath

    private static class GrettyWebSocketHandlerAroundClosure extends GrettyWebSocketHandler {
        Closure closure
        GrettyWebSocketHandlerAroundClosure(Closure closure) {
            this.closure = closure
        }
        void onMessage(String message) {
            closure(message)
        }

        GrettyWebSocketHandler clone() {
            GrettyWebSocketHandlerAroundClosure cloned = super.clone ()
            Closure clonedClosure = closure.clone ()

            cloned.closure = clonedClosure
            clonedClosure.delegate = cloned
            clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST

            cloned
        }
    }

    static GrettyWebSocketHandler fromClosure(Closure closure) {
        new GrettyWebSocketHandlerAroundClosure(closure)
    }

    void connect(GrettyWebSocket socket, HttpRequest request) {
        this.socket = socket
        onConnect(request)
    }

    void onConnect(HttpRequest request) {}

    GrettyWebSocketHandler clone() {
        return super.clone()
    }
}
