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

import org.jboss.netty.channel.*
import org.jboss.netty.handler.codec.http.*
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.logging.InternalLogLevel

import org.mbte.gretty.AbstractServer
import java.util.concurrent.Executor
import groovypp.concurrent.BindLater
import groovypp.concurrent.CallLater
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.GeneratedClosure
import org.mbte.gretty.httpserver.session.GrettySessionManager
import org.mbte.gretty.httpserver.template.GrettyTemplateEngine

@Typed class GrettyServer extends AbstractServer {
    GrettyContext defaultContext

    GrettySessionManager sessionManager

    InternalLogLevel logLevel

    GrettyServer() {
        localAddress = new InetSocketAddress(8080)
    }

    void setUnresolvedProperty(String name, GrettyRestDescription description) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.setUnresolvedProperty(name, description)
    }

    void setDefault (GrettyHttpHandler handler) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.default = handler
    }

    void setStatic (String staticFiles) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.static = staticFiles
    }

    void setStaticResources(String staticResources) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.staticResources = staticResources
    }

    void setPublic (GrettyPublicDescription description) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.public = description
    }

    void setWebContexts(Map<String,GrettyContext> webContexts) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.webContexts = webContexts
    }

    void start () {
        if(!defaultContext)
            throw new IllegalStateException("No root context configured")
        defaultContext.initContext ("/", null, this)
        super.start ()
    }

     protected void buildPipeline(ChannelPipeline pipeline) {
        super.buildPipeline(pipeline)

        pipeline.addLast("flash.policy.file", new FlashPolicyFileHandler(this))

        pipeline.addLast("http.request.decoder", new GrettyRequestDecoder())
        pipeline.addLast("http.response.encoder", new GrettyResponseEncoder())

        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler())
        pipeline.addLast("fileWriter", new FileWriteHandler())

        def logger = logLevel ? new HttpLoggingHandler(logLevel) : null
        if (logger)
            pipeline.addLast("http.logger", logger)

        pipeline.addLast("http.application", new GrettyAppHandler(this))
    }

    final <S> BindLater<S> async(Executor executor = null, CallLater<S> action) {
        if(!executor)
            executor = threadPool
        executor.callLater(action)
    }

    final void setGroovy(Map<String,Object> properties) {
        def mc = InvokerHelper.getMetaClass(this.class)
        for(e in properties.entrySet()) {
            def property = mc.getMetaProperty(e.key)
            if(!property)
                throw new RuntimeException("No such property '${e.key}'")

            if(e.value instanceof GeneratedClosure) {
                property.setProperty(this, property.type.getDeclaredMethod("fromClosure", Closure).invoke(null, e.value))
            }
            else
                property.setProperty(this, e.value)
        }
    }
}
