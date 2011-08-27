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

import org.jboss.netty.channel.*

import org.jboss.netty.handler.stream.ChunkedWriteHandler

import org.mbte.gretty.AbstractServer
import java.util.concurrent.Executor
import groovypp.concurrent.BindLater
import groovypp.concurrent.CallLater
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.GeneratedClosure
import org.mbte.gretty.httpserver.session.GrettySessionManager

import org.mbte.gretty.httpserver.session.GrettyInMemorySessionManager
import org.jboss.netty.logging.InternalLoggerFactory
import org.jboss.netty.logging.InternalLogger
import org.mbte.gretty.httpclient.HttpRequestHelper
import org.jboss.netty.handler.codec.http.HttpChunkAggregator

@Typed class GrettyServer extends AbstractServer<GrettyServer> implements HttpRequestHelper {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GrettyServer)

    GrettyContext defaultContext

    GrettySessionManager sessionManager

    GrettyServer() {
        sessionManager = new GrettyInMemorySessionManager()
    }

    void setUnresolvedProperty(String name, GrettyRestDescription description) {
        handler(name, description)
    }

    GrettyServer handler(String name, GrettyRestDescription description) {
        if (!defaultContext)
            defaultContext = []
        defaultContext.setUnresolvedProperty(name, description)
        this
    }

    void addWebContext (String name, GrettyContext context) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.addWebContext(name, context)
        this
    }

    GrettyServer webContext (String name, GrettyContext context) {
        addWebContext(name, context)
        this
    }

    GrettyServer webContext (String name, String path) {
        addWebContext(name, new GrettyContext(path))
        this
    }

    GrettyHttpHandler getDefaultHandler () {
        defaultContext?.defaultHandler
    }

    void setDefaultHandler (GrettyHttpHandler handler) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.defaultHandler = handler
    }

    GrettyServer defaultHandler(GrettyHttpHandler handler) {
        this[defaultHandler: handler]
    }

    def methodMissing(String name, Object _args) {
        Object [] args = _args
        switch(name) {
            case "defaultHandler":
                if(args?.length == 1 && args[0] instanceof Closure) {
                    return defaultHandler( GrettyHttpHandler.fromClosure((Closure)args[0]) )
                }
            break

            case "doTest":
                if(args?.length == 2 && args[1] instanceof Closure) {
                    if(args[0] instanceof GrettyHttpRequest)
                        return doTest(((GrettyHttpRequest)args[0])) { resp -> ((Closure)args[1])(resp) }
                    else
                        return doTest(args[0].toString()) { resp -> ((Closure)args[1])(resp) }
                }

                if(args?.length == 3 && args[1] instanceof SocketAddress && args[2] instanceof Closure) {
                    if(args[0] instanceof GrettyHttpRequest)
                        return doTest((GrettyHttpRequest)args[0], (SocketAddress)args[1]) { resp -> ((Closure)args[2])(resp) }
                    else
                        return doTest(args[0].toString(), (SocketAddress)args[1]) { resp -> ((Closure)args[2])(resp) }
                }
            break
        }

        if(args?.length == 1 && args[0] instanceof Closure) {
            return handler(name, GrettyRestDescription.fromClosure((Closure)args[0]))
        }

        throw new MissingMethodException(name, this.class, args)
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

    GrettyServer dir(String path) {
        this[dir: path]
    }

    void setDir(String path) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.dir = path
    }

    void setWebContexts(Map<String,GrettyContext> webContexts) {
        if(!defaultContext)
            defaultContext = []
        defaultContext.webContexts = webContexts
    }

    GrettyServer webContexts(Map<String,GrettyContext> webContexts) {
        this[webContexts: webContexts]
    }

    void start () {
        if(!defaultContext)
            throw new IllegalStateException("No root context configured")
        defaultContext.initContext ("/", null, this)

        sessionManager.server = this
        sessionManager?.start()

        super.start ()
    }

    void stop() {
        super.stop()

        sessionManager?.stop ()
        sessionManager.server = null
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        super.buildPipeline(pipeline)

        pipeline.addLast("flash.policy.file", new FlashPolicyFileHandler(this))

        pipeline.addLast("http.request.decoder", new GrettyRequestDecoder())
        pipeline.addLast("http.request.aggregator", new HttpChunkAggregator(Integer.MAX_VALUE))

        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler())
        pipeline.addLast("http.response.encoder", new GrettyResponseEncoder())

        pipeline.addLast("fileWriter", new FileWriteHandler())

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
            if(!property && e.value instanceof Closure) {
                setUnresolvedProperty(e.key, GrettyRestDescription.fromClosure((Closure)e.value))
                return
            }

            if(e.value instanceof GeneratedClosure) {
                property.setProperty(this, property.type.getDeclaredMethod("fromClosure", Closure).invoke(null, e.value))
            }
            else
                property.setProperty(this, e.value)
        }
    }

    /**
     * for HttpRequestHelper.doTest()
     * @return local address of the server
     */
    SocketAddress getTestServerAddress () {
        localAddress
    }
}
