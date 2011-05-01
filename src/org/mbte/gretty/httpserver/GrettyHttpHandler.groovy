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

import freemarker.template.Configuration
import freemarker.template.ObjectWrapper
import org.codehaus.jackson.map.ObjectMapper
import org.mbte.gretty.JacksonCategory
import groovypp.concurrent.BindLater
import groovypp.concurrent.CallLater
import java.util.concurrent.Executor

@Typed
@Use(JacksonCategory)
abstract class GrettyHttpHandler implements Cloneable {
    GrettyHttpRequest  request
    GrettyHttpResponse response
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

        final String template(String file, Map root = [:], Closure dataBinding) {
            template(file, root, (Function1<Map,Void>){ binding -> dataBinding(binding) } )
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
        clone.handle (pathArgs)
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

    final String template(String file, Map root = [:], Function1<Map,Void> dataBinding = null) {
        root.request = request

        dataBinding?.call(root)

        def template = Configuration.getDefaultConfiguration().getTemplate(file)
        def writer = new StringWriter()
        template.process (root, writer, ObjectWrapper.BEANS_WRAPPER)
        writer.toString()
    }

    abstract void handle(Map<String,String> pathArguments)
}
