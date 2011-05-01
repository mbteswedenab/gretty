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

import org.jboss.netty.channel.Channel
import java.util.concurrent.ConcurrentHashMap
import groovypp.channels.ExecutingChannel
import org.jboss.netty.channel.ChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.jboss.netty.channel.ChannelStateEvent
import groovypp.channels.MessageChannel
import org.jboss.netty.handler.codec.http.HttpMessageEncoder
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.channel.ChannelState
import org.mbte.gretty.JacksonCategory

@Typed abstract class GrettyWebSocketHandler implements Cloneable {
    protected GrettyServer server

    private GrettyWebSocket socket

    protected ConcurrentHashMap<Channel,GrettyWebSocketHandler> allConnected = [:]

    protected String socketPath

    /**
     * @param message GrettyWebSocketEvent.CONNECT | GrettyWebSocketEvent.DISCONNECT | String
     */
    abstract void onEvent(Object message)

    final void send(Object object) {
        socket.channel.write(new DefaultWebSocketFrame(object.toString()))
    }

    final void broadcast(Object object) {
        for(e in allConnected.entrySet()) {
            e.value.send object
        }
    }

    protected void initConnection(ChannelHandlerContext ctx, GrettyHttpResponse response) {
        def channel = ctx.channel
        socket = new GrettyWebSocket()[handler: this, channel: channel, executor: server.threadPool]
        allConnected [channel] = this
        channel.pipeline.addLast "websocket.handler", socket

        GrettyResponseEncoder enc = channel.pipeline.remove("http.response.encoder")
        def respToSend = enc.encode(ctx, channel, response)

        channel.write(respToSend).addListener {
            // we want to make sure that we connect only after response sent
            socket << GrettyWebSocketEvent.CONNECT
        }
    }

    private static class GrettyWebSocketHandlerAroundClosure extends GrettyWebSocketHandler {
        Closure closure

        GrettyWebSocketHandlerAroundClosure(Closure closure) {
            this.closure = closure
        }

        void onEvent(Object message) {
            use(JacksonCategory) {
                closure(message)
            }
        }

        GrettyWebSocketHandler clone() {
            GrettyWebSocketHandlerAroundClosure cloned = super.clone ()
            Closure clonedClosure = closure.clone ()

            cloned.closure = clonedClosure
            clonedClosure.delegate = cloned
            clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST

            cloned
        }
    }

    static GrettyWebSocketHandler fromClosure(Closure closure) {
        new GrettyWebSocketHandlerAroundClosure(closure)
    }

    GrettyWebSocketHandler clone() {
        return super.clone()
    }

    private static class GrettyWebSocket extends ExecutingChannel implements ChannelUpstreamHandler {
        protected Channel channel

        private int connectStatus

        protected GrettyWebSocketHandler handler

        void close () {
            channel.close()
        }

        protected void onMessage(Object message) {
            // it might happen that 1st message from other side came earlier than we received CONNECT
            if(message == GrettyWebSocketEvent.CONNECT) {
                if(connectStatus) {
                    connectStatus = 1
                    handler.onEvent message
                }
            }
            else {
                if(message == GrettyWebSocketEvent.DISCONNECT) {
                    if(connectStatus == 1) {
                        connectStatus = 2
                        handler.onEvent message
                    }
                }
                else {
                    if(connectStatus == 0) {
                        connectStatus = 1
                        handler.onEvent GrettyWebSocketEvent.CONNECT
                    }

                    if(connectStatus == 1) {
                        handler.onEvent message
                    }
                }
            }
        }

        void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {
            switch(e) {
                case MessageEvent:
                    WebSocketFrame frame = e.message
                    post frame.textData
                break

                case ChannelStateEvent:
                    if(e.state == ChannelState.CONNECTED && !e.value) {
                        handler.allConnected.remove channel
                        post GrettyWebSocketEvent.DISCONNECT
                    }
                break
            }
        }
    }
}
