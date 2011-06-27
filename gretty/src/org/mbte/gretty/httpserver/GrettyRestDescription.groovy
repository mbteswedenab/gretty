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

import org.jboss.netty.handler.codec.http.HttpMethod

@Typed abstract class GrettyRestDescription implements Runnable {
    GrettyContext context
    String        match

    private static class GrettyRestDescriptionAroundClosure extends GrettyRestDescription {
        Closure closure

        GrettyRestDescriptionAroundClosure(Closure closure) {
            this.closure = closure
            closure.delegate = this
            closure.resolveStrategy = Closure.DELEGATE_FIRST
        }

        void run () {
            closure.run ()
        }

        void options(Closure handler) {
            options(GrettyHttpHandler.fromClosure(handler))
        }

        void get(Closure handler) {
            get(GrettyHttpHandler.fromClosure(handler))
        }

        void head(Closure handler) {
            head(GrettyHttpHandler.fromClosure(handler))
        }

        void post(Closure handler) {
            post(GrettyHttpHandler.fromClosure(handler))
        }

        void put(Closure handler) {
            put(GrettyHttpHandler.fromClosure(handler))
        }

        void patch(Closure handler) {
            patch(GrettyHttpHandler.fromClosure(handler))
        }

        void delete(Closure handler) {
            delete(GrettyHttpHandler.fromClosure(handler))
        }

        void trace(Closure handler) {
            trace(GrettyHttpHandler.fromClosure(handler))
        }

        void connect(Closure handler) {
            connect(GrettyHttpHandler.fromClosure(handler))
        }
    }

    void options(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.OPTIONS, match, handler)
    }

    void get(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.GET, match, handler)
    }

    void head(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.HEAD, match, handler)
    }

    void post(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.POST, match, handler)
    }

    void put(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.PUT, match, handler)
    }

    void patch(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.PATCH, match, handler)
    }

    void delete(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.DELETE, match, handler)
    }

    void trace(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.TRACE, match, handler)
    }

    void connect(GrettyHttpHandler handler) {
        context.addHandler(HttpMethod.CONNECT, match, handler)
    }

    static GrettyRestDescription fromClosure(Closure closure) {
        new GrettyRestDescriptionAroundClosure(closure)
    }
}
