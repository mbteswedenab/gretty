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

package org.mbte.gretty

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.Channels

/**
 * Base class for client and server
 * @param < OwnType >
 */
@Typed class BaseChannelHandler<OwnType> extends SimpleChannelHandler implements ChannelPipelineFactory {

    protected volatile Channel channel

    SocketAddress localAddress

    static abstract class GrettyChannelEventHandler<E> {
        abstract void onEvent(ChannelHandlerContext ctx, E e)
    }

    final ChannelPipeline getPipeline () {
        def pipeline = Channels.pipeline()
        buildPipeline(pipeline)
        pipeline
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst("this", this)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        super.messageReceived(ctx, e)
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        super.exceptionCaught(ctx, e)
    }

    void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        super.channelConnected(ctx, e)
    }

    void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        super.channelDisconnected(ctx, e)
    }

    void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        super.channelClosed(ctx, e)
    }

    final OwnType localPort(int port) {
        localAddress(new InetSocketAddress(port))
    }

    final OwnType localAddress(SocketAddress localAddress) {
        this[localAddress: localAddress]
    }

    OwnType onMessageReceived(GrettyChannelEventHandler<MessageEvent> handler) {
    }

    OwnType onExceptionCaught(GrettyChannelEventHandler<ExceptionEvent> handler) {
    }

    OwnType onChannelConnected(GrettyChannelEventHandler<ChannelStateEvent> handler) {
    }

    OwnType onChannelDisconnected(GrettyChannelEventHandler<ChannelStateEvent> handler) {
    }

    OwnType onChannelClosed(GrettyChannelEventHandler<ChannelStateEvent> handler) {
    }
}
