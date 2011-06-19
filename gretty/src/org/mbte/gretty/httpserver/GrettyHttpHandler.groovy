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

import org.mbte.gretty.JacksonCategory
import groovypp.concurrent.BindLater
import groovypp.concurrent.CallLater
import java.util.concurrent.Executor
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.mbte.gretty.httpserver.template.GrettyTemplateScript
import groovypp.text.FastStringWriter
import org.mbte.gretty.httpserver.session.GrettySession
import org.mbte.gretty.httpserver.session.GrettySessionManager

@Typed
@Use(JacksonCategory)
abstract class GrettyHttpHandler implements Cloneable {
    static ThreadLocal<GrettyHttpHandler> grettyHandler = []

    GrettyHttpRequest  request
    GrettyHttpResponse response
    GrettyContext      context
    GrettyServer       server

    private static class GrettyHttpHandlerAroundClosure extends GrettyHttpHandler {
        Closure closure

        GrettyHttpHandlerAroundClosure(Closure closure) {
            this.closure = closure
            closure.delegate = this
            closure.resolveStrategy = Closure.DELEGATE_FIRST
        }

        void handle(Map<String, String> pathArguments) {
            closure(pathArguments)
        }

        protected def clone() {
            GrettyHttpHandlerAroundClosure cloned = super.clone ()
            Closure clonedClosure = closure.clone ()

            cloned.closure = clonedClosure
            clonedClosure.delegate = cloned
            clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST

            cloned
        }

        final String template(String file, Closure dataBinding = null) {
            template(file, [:], dataBinding )
        }

        final String template(String file, Map root, Closure dataBinding = null) {
            Function1<Map, Object> function1 = { binding -> dataBinding.call(binding); return null }
            template(file, root, function1 )
        }

        GrettyHttpHandler getThisHandler () {
            this
        }

        final BindLater async(Executor executor = null, Closure action) {
            async(executor, (CallLater){ action() } )
        }
    }

    static GrettyHttpHandler fromClosure(Closure closure) {
        new GrettyHttpHandlerAroundClosure(closure)
    }

    final void handle (GrettyHttpRequest request, GrettyHttpResponse response, Map<String,String> pathArgs) {
        GrettyHttpHandler clone = clone ()
        clone.request  = request
        clone.response = response

        def saved = grettyHandler.get()
        grettyHandler.set(clone)
        try {
            clone.handle (pathArgs)
        }
        finally {
            grettyHandler.set(saved)
        }
    }

    final void execute(Runnable command) {
        response.async.incrementAndGet()
        server.execute {
            command.run()
            if(!response.async.decrementAndGet())
                response.complete()
        }
    }

    final <S> BindLater<S> async(Executor executor = null, CallLater<S> action, BindLater.Listener<S> whenDone = null) {
        if(whenDone)
            action.whenBound whenDone

        response.async.incrementAndGet()
        server.async(executor){
            def res = action.run()
            if(!response.async.decrementAndGet())
                response.complete()
            res
        }
    }

    final void redirect(String where) {
        response.redirect(where)
    }

    String template(String file, Map root = [:], Function1<Map,Object> dataBinding = null) {
        root.request = request

        dataBinding?.call(root)

        def template = context.templateEngine.getTemplateClass([file])
        GrettyTemplateScript script = template.newInstance() [grettyHttpHandler: this]
        def writer = new FastStringWriter()
        script.writeTo(root, writer)
        writer.toString()
    }

    Function1 asyncFunction(Function1 f) {
        response.async.incrementAndGet()
        return { t ->
            try {
                f(t)
            }
            catch(e) {
                response[text: e.message, status: HttpResponseStatus.INTERNAL_SERVER_ERROR]
            }

            if(!response.async.decrementAndGet())
                response.complete()
        }
    }

    abstract void handle(Map<String,String> pathArguments)

    GrettySession getSession(boolean force = true) {
        def res = response.session
        if(!res) {
            def cookie = request.getCookie("JSESSIONID")
            if(cookie) {
                res = response.session = server.sessionManager.getSession(cookie.value)
            }
            else {
                if(force) {
                    res = new GrettySession()[id: UUID.randomUUID(), server: server]
                    server.sessionManager.storeSession(res)
                    response.session = res
                }
            }
        }

        if(res)
            res.lastAccessedTime = System.currentTimeMillis()
        res
    }

    BindLater<GrettySession> getSessionAsync(GrettySessionManager.SessionCallback callback) {
        def res = response.session
        if(!res) {
            def cookie = request.getCookie("JSESSIONID")
            if(cookie) {
                if(callback) {
                    callback.whenBound { bl ->
                        def session = bl.get()
                        response.session = session
                        session.lastAccessedTime = System.currentTimeMillis()
                    }
                }
                else {
                    callback = { session ->
                        response.session = session
                        session.lastAccessedTime = System.currentTimeMillis()
                    }
                }
                return server.sessionManager.getSessionAsync(cookie.value, callback)
            }
            else {
                res = new GrettySession()[id: UUID.randomUUID(), server: server]
                res.lastAccessedTime = System.currentTimeMillis()
                response.session = res

                def later = callback ?: new BindLater()
                later.set(res)
                later
            }
        }
    }
}
