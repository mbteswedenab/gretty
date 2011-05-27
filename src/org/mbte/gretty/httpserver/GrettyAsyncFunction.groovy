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

import org.jboss.netty.handler.codec.http.HttpResponseStatus

@Trait abstract class GrettyAsyncFunction<T,R> {
    GrettyHttpHandler handler = initHandler()

    GrettyHttpRequest  getRequest  () { handler.request }

    GrettyHttpResponse getResponse () { handler.response }

    abstract R doAsyncAction(T arg);

    static GrettyHttpHandler initHandler() {
        def handler = GrettyHttpHandler.grettyHandler.get()
        handler.response.async.incrementAndGet ()
        handler
    }

    void handlerAction(T t) {
        GrettyHttpHandler.grettyHandler.set(handler)

        try {
            doAsyncAction(t)
        }
        catch(e) {
            response.text = e.message
            response.status = HttpResponseStatus.INTERNAL_SERVER_ERROR
        }

        GrettyHttpHandler.grettyHandler.set(null)
        if(!response.async.decrementAndGet())
            response.complete()
    }
}

