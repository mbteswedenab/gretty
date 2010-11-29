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

package org.mbte.gretty

import java.util.concurrent.Executors
import java.util.concurrent.Executor

import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.Channel
import org.jboss.netty.util.internal.ExecutorUtil
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.ChannelPipelineFactory
import java.util.concurrent.ExecutorService
import org.mbte.gretty.httpserver.IoMonitor

@Typed abstract class AbstractServer extends SimpleChannelHandler implements Executor, ChannelPipelineFactory {
    IoMonitor ioMonitor = []

    int              ioWorkerCount      = 2*Runtime.getRuntime().availableProcessors()
    int              serviceWorkerCount = 4*Runtime.getRuntime().availableProcessors()

    SocketAddress    localAddress

    protected Channel channel

    final DefaultChannelGroup allConnected = []

    protected ExecutorService threadPool

    void start () {
        def bossExecutor = Executors.newCachedThreadPool()
        def ioExecutor   = Executors.newFixedThreadPool(ioWorkerCount)
        threadPool       = Executors.newFixedThreadPool(serviceWorkerCount)

        assert localAddress
        def isLocal = localAddress instanceof LocalAddress

        def channelFactory = isLocal ? new DefaultLocalServerChannelFactory () : (NioServerSocketChannelFactory )[bossExecutor, ioExecutor]

        ServerBootstrap bootstrap = [factory: channelFactory, pipelineFactory:this]
        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive",  true)

        channel = bootstrap.bind(localAddress)
        channel.closeFuture.addListener {
            [bossExecutor, ioExecutor, threadPool]*.shutdown()
        }
    }

    void stop() {
        allConnected.close().awaitUninterruptibly()
        channel.close().awaitUninterruptibly()
    }

    void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        allConnected.add(ctx.channel)
        super.channelConnected(ctx, e)
    }

    void execute(Runnable command) {
        threadPool.execute command
    }

    ChannelPipeline getPipeline() {
        def pipeline = Channels.pipeline()
        buildPipeline(pipeline)
        pipeline
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst ("serverItself", this)
        pipeline.addFirst ("ioMonitor", ioMonitor)
    }
}
