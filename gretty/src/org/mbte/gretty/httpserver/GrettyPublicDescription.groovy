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
import org.codehaus.groovy.runtime.InvokerHelper

@Typed abstract class GrettyPublicDescription implements Runnable {
    GrettyContext context

    private static class GrettyPublicDescriptionAroundClosure extends GrettyPublicDescription {
        Closure closure

        GrettyPublicDescriptionAroundClosure(Closure closure) {
            this.closure = closure
            closure.delegate = this
            closure.resolveStrategy = Closure.DELEGATE_FIRST
        }

        void run () {
            closure.run ()
        }

        void options(String match, Closure handler) {
            options(match, GrettyHttpHandler.fromClosure(handler))
        }

        void get(String match, Closure handler) {
            get(match, GrettyHttpHandler.fromClosure(handler))
        }

        void head(String match, Closure handler) {
            head(match, GrettyHttpHandler.fromClosure(handler))
        }

        void post(String match, Closure handler) {
            post(match, GrettyHttpHandler.fromClosure(handler))
        }

        void put(String match, Closure handler) {
            put(match, GrettyHttpHandler.fromClosure(handler))
        }

        void patch(String match, Closure handler) {
            patch(match, GrettyHttpHandler.fromClosure(handler))
        }

        void delete(String match, Closure handler) {
            delete(match, GrettyHttpHandler.fromClosure(handler))
        }

        void trace(String match, Closure handler) {
            trace(match, GrettyHttpHandler.fromClosure(handler))
        }

        void connect(String match, Closure handler) {
            connect(match, GrettyHttpHandler.fromClosure(handler))
        }

        void websocket(String match, def shared = null, Closure handler) {
            websocket(match, shared, GrettyWebSocketHandler.fromClosure(handler))
        }

        public void iosocket(String match, def shared, Closure handler) {
            iosocket(match, shared, GrettyIoSocketHandler.fromClosure(handler))
        }
    }

    static GrettyPublicDescription fromClosure(Closure closure) {
        new GrettyPublicDescriptionAroundClosure(closure)
    }

    void invokeUnresolvedMethod(String name, GrettyContext childContext) {
        context.webContexts[name] = childContext
    }

    GrettyHttpHandler options(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.OPTIONS, match, handler)
    }

    GrettyHttpHandler get(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.GET, match, handler)
    }

    GrettyHttpHandler head(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.HEAD, match, handler)
    }

    GrettyHttpHandler post(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.POST, match, handler)
    }

    GrettyHttpHandler put(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.PUT, match, handler)
    }

    GrettyHttpHandler patch(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.PATCH, match, handler)
    }

    GrettyHttpHandler delete(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.DELETE, match, handler)
    }

    GrettyHttpHandler trace(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.TRACE, match, handler)
    }

    GrettyHttpHandler connect(String match, GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.CONNECT, match, handler)
    }

    public <S> void websocket(String match, S shared = null, GrettyWebSocketHandler<S,Object> handler) {
        websocket(match, shared, Object, handler)
    }

    public <S,P> void websocket(String match, S shared, Class<P> _private, GrettyWebSocketHandler<S,P> handler) {
        context.addWebSocket(match, handler[socketShared: shared])
    }

    public <S,P> void iosocket(String match, S shared = null, GrettyIoSocketHandler<S,P> handler) {
        iosocket(match, shared, Object, handler)
    }

    public <S,P> void iosocket(String match, S shared, Class<P> _private, GrettyIoSocketHandler<S,P> handler) {
        websocket("$match/websocket",   shared, handler.clone())
        websocket("$match/flashsocket", shared, handler.clone())
    }

    void rest(String match, GrettyRestDescription descr) {
        descr.context = context
        descr.match = match
        descr.run ()
    }
}
