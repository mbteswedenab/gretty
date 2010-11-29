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

import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.ChannelHandlerContext
import org.mbte.gretty.AbstractClient
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelPipeline
import groovypp.concurrent.BindLater
import java.nio.ByteBuffer
import groovypp.concurrent.FQueue
import groovypp.concurrent.BindLater.Listener
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import groovypp.concurrent.CallLaterExecutors
import org.jboss.netty.channel.ChannelFactory

@Typed class MapClient extends AbstractClient {

    private volatile FQueue<Command> sent = FQueue.emptyQueue

    MapClient(ChannelFactory factory = null) {
        super(new InetSocketAddress(8080), factory);
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.cause.printStackTrace()
        super.exceptionCaught(ctx, e)
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("encoder",   new CommandEncoder())
        pipeline.addLast("decoder",   new ResponseDecoder())
        pipeline.addLast("processor", this)
        pipeline
    }

    static class CommandEncoder extends OneToOneEncoder {
        protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) {
            msg instanceof Command ? ((Command)msg).encode() : msg
        }
    }

    class ResponseDecoder extends FrameDecoder {
        protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
            def cmd = sent.first
            def pos = buffer.readerIndex()

            def res = cmd.decodeResponse(buffer)
            if(res != null) {
                for(;;) {
                    def s = sent
                    if(sent.compareAndSet(s, s.removeFirst().second)) {
                        cmd.set(res)
                        return res
                    }
                }
            }
            else {
                buffer.readerIndex(pos)
                return null
            }
        }
    }

    BindLater<byte[]> get(String key, BindLater.Listener<byte[]> listener = null) {
        enqueue((GET)[key:key], listener)
    }

    BindLater<Void> set(String key, byte[] value, BindLater.Listener<Void> listener = null) {
        enqueue((SET)[key:key, value:value], listener)
    }

    public <C extends Command> C enqueue(C command, BindLater.Listener<byte[]> listener) {
        if(listener)
            command.whenBound(listener.async())

        for(;;) {
            def s = sent
            if(sent.compareAndSet(s, s + command)) {
                channel.write(command)
                return command
            }
        }
    }
}
