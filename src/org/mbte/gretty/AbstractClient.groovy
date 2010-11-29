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

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel.*
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.local.LocalClientChannelFactory
import org.jboss.netty.channel.socket.ClientSocketChannelFactory

@Typed class AbstractClient extends SimpleChannelHandler implements ChannelPipelineFactory {
    protected Channel channel

    protected final SocketAddress remoteAddress

    protected final ChannelFactory channelFactory
    protected final boolean shouldReleaseResources = true

    AbstractClient(SocketAddress remoteAddress, ChannelFactory channelFactory = null) {
        this.remoteAddress = remoteAddress
        shouldReleaseResources = (channelFactory == null)
        if(shouldReleaseResources) {
            if(remoteAddress instanceof LocalAddress) {
                channelFactory = new DefaultLocalClientChannelFactory()
            }
            else {
                channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
            }
        }
        else {
            if(remoteAddress instanceof LocalAddress) {
                assert channelFactory instanceof LocalClientChannelFactory
            }
            else {
                assert channelFactory instanceof NioClientSocketChannelFactory
            }
        }
        this.channelFactory = channelFactory
    }

    ChannelFuture connect () {
        ClientBootstrap bootstrap = [channelFactory]
        bootstrap.pipelineFactory = this
        bootstrap.setOption("tcpNoDelay", true)
        bootstrap.setOption("keepAlive",  true)

        def connectFuture = bootstrap.connect(remoteAddress)
        channel = connectFuture.channel

        def returnFuture = Channels.future(channel)
        connectFuture.addListener { future ->
            if(future.success) {
                try {
                    onConnect ()
                }
                catch(Throwable t) {
                    returnFuture.setFailure(t)
                    channel.close()
                    return
                }
                returnFuture.setSuccess()
            }
            else {
               future.channel.close ()
               try {
                   onConnectFailed (future.cause)
               }
               catch(Throwable ignore) { //
               }
               returnFuture.setFailure(future.cause)
            }

            if(shouldReleaseResources) {
                future.channel.closeFuture.addListener { future2 ->
                    future2.channel.factory.releaseExternalResources()
                }
            }
        }
        returnFuture
    }

    void connect(ChannelFutureListener listener) {
        if(!connected)
            connect().addListener listener
        else
            listener.operationComplete(Channels.future(channel))
    }

    boolean isConnected () {
        channel?.connected
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        ctx.channel.close ()
    }

    void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        if(channel != null) {
            channel = null
            onDisconnect()
        }
    }

    void disconnect() {
        channel?.close()
    }

    protected void onConnect () {}

    protected void onConnectFailed (Throwable cause) {}

    protected void onDisconnect () {}

    ChannelPipeline getPipeline () {
        def pipeline = Channels.pipeline()
        buildPipeline(pipeline)
        pipeline
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst("clientItself", this)
    }

    ChannelFuture write(Object message) {
        channel.write(message)
    }

    void close() {
        channel?.close()
    }
}
