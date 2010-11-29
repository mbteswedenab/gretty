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

@Typed class GrettyServer extends AbstractServer {
    GrettyContext defaultContext

    InternalLogLevel logLevel

    Map<String,GrettyContext> webContexts = [:]

    final PseudoWebSocketManager pseudoWebSocketManager = []

    GrettyServer() {
        localAddress = new InetSocketAddress(8080)
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

    private void initContexts () {
        if(defaultContext) {
            if(!webContexts["/"])
                webContexts["/"] = defaultContext
            else {
                if(!webContexts["/"].defaultHandler)
                    webContexts["/"].defaultHandler = defaultContext.defaultHandler
                else
                    throw new IllegalStateException("Default handler already set")
            }

            defaultContext = null
        }
        webContexts = webContexts.sort { me1, me2 -> me2.key <=> me1.key }

        for(e in webContexts.entrySet()) {
            e.value.initContext(e.key, this)
        }
    }

    void start () {
        initContexts ()
        super.start ()
    }

     protected void buildPipeline(ChannelPipeline pipeline) {
        super.buildPipeline(pipeline)

        pipeline.addLast("flash.policy.file", new FlashPolicyFileHandler(this))

        pipeline.addLast("http.request.decoder", new GrettyRequestDecoder())
        pipeline.addLast("http.request.encoder", new HttpResponseEncoder())

        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler())
        pipeline.addLast("fileWriter", new FileWriteHandler())

        def logger = logLevel ? new HttpLoggingHandler(logLevel) : null
        if (logger)
            pipeline.addLast("http.logger", logger)

        pipeline.addLast("http.application", new GrettyAppHandler(this))
    }
}
