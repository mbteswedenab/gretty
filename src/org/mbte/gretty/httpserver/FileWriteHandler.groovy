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

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.channel.DefaultFileRegion
import org.jboss.netty.channel.ChannelDownstreamHandler
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.Channels
import org.jboss.netty.handler.stream.ChunkedStream
import org.jboss.netty.buffer.ChannelBuffers

@Typed class FileWriteHandler implements ChannelDownstreamHandler {
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        switch(evt) {
            case MessageEvent:
                def msg = evt.message
                switch(msg) {
                    case File:
                        if (evt.channel.pipeline.get(SslHandler.class) != null) {
                            Channels.write(ctx, evt.future, new ChunkedFile(msg, 8192), evt.remoteAddress)
                        } else {
                            RandomAccessFile ras = [msg, "r"]
                            DefaultFileRegion region = [ras.channel, 0, ras.length()]
                            evt.future.addListener{ region.releaseExternalResources() }
                            Channels.write(ctx, evt.future, region, evt.remoteAddress)
                        }
                        break

                    default:
                        ctx.sendDownstream(evt)
                }
            break

            default:
                ctx.sendDownstream(evt)
        }
    }
}
