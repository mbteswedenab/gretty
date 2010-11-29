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
package org.mbte.gretty.memserver

import org.mbte.gretty.AbstractServer

import java.util.concurrent.ConcurrentHashMap

import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.frame.FrameDecoder

@Typed class MapServer extends AbstractServer {
    final ConcurrentHashMap<String,byte[]> map = [:]

    MapServer() {
        localAddress = new InetSocketAddress(8080)

        serviceWorkerCount = 32
        ioWorkerCount = 32
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("decoder",   new CommandDecoder())
        pipeline.addLast("processor", this)
        pipeline
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Command msg = e.message
        msg.channel = ctx.channel
        msg.execute(map)
    }

    static class CommandDecoder extends FrameDecoder {
        protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
            if(buffer.readableBytes() < 4)
                return null

            def code = Integer.valueOf(buffer.readInt ())
            Command.commandDecoders[code].decode(buffer)
        }
    }
}
