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

import org.jboss.netty.channel.Channel
import java.util.concurrent.ConcurrentHashMap
import groovypp.channels.ExecutingChannel
import org.jboss.netty.channel.ChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.jboss.netty.channel.ChannelStateEvent

import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.channel.ChannelState
import org.mbte.gretty.JacksonCategory

@Typed abstract class GrettyWebSocketHandler<S,P> implements Cloneable {
    protected GrettyServer server

    protected Connection socket

    protected ConcurrentHashMap<Channel,GrettyWebSocketHandler> allConnected = [:]

    protected S socketShared

    protected P socketPrivate

    protected String socketPath

    abstract void onMessage(Object message)

    protected void onConnect () {}

    protected void onDisconnect () {}

    protected void send(Object object) {
        sendInternal(object)
    }

    protected void sendJson(data) {
        send(JacksonCategory.toJsonString(data))
    }

    protected void sendInternal(object) {
        socket.channel.write(new DefaultWebSocketFrame(object?.toString()))
    }

    protected void broadcast(Object object) {
        for(e in allConnected.entrySet()) {
            e.value.sendInternal object
        }
    }

    protected void initConnection(ChannelHandlerContext ctx, GrettyHttpResponse response) {
        def channel = ctx.channel
        if(!socket)
            socket = createSocket(channel)
        if(!allConnected.containsKey(channel))
            allConnected [channel] = this
        channel.pipeline.addLast "websocket.handler", socket

        GrettyResponseEncoder enc = channel.pipeline.remove("http.response.encoder")
        def respToSend = enc.encode(ctx, channel, response)

        channel.write(respToSend).addListener {
            // we want to make sure that we connect only after response sent
            socket << GrettyWebSocketEvent.CONNECT
        }
    }

    protected Connection createSocket(Channel channel) {
        def res = new Connection()[handler: this, channel: channel, executor: server.threadPool]
        allConnected [channel] = this
        return res
    }

    protected static class WrappedClosure extends HashMap<String,Closure> implements Cloneable {
        Closure onConnectClosure, onDisconnectClosure, onMessageClosure

        WrappedClosure(Closure closure) {
            this.handler = handler

            def that = this
            closure.delegate = (GroovyObjectSupport)[
                onMessage: { Closure define ->
                    onMessageClosure = define
                },

                onConnect: { Closure define ->
                    onConnectClosure = define
                },

                onDisconnect: { Closure define ->
                    onDisconnectClosure = define
                }
            ]
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.run()
        }

        void onMessage(Object message) {
            use(JacksonCategory) {
                onMessageClosure?.call(message)
            }
        }

        void onConnect() {
            use(JacksonCategory) {
                onConnectClosure?.call()
            }
        }

        void onDisconnect() {
            use(JacksonCategory) {
                onDisconnectClosure?.call()
            }
        }

        WrappedClosure clone(GrettyWebSocketHandler handler) {
            WrappedClosure cloned = super.clone ()
            Closure clonedClosure

            if(onMessageClosure) {
                clonedClosure = onMessageClosure.clone ()
                clonedClosure.delegate = handler
                clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
                cloned.onMessageClosure = clonedClosure
            }

            if(onConnectClosure) {
                clonedClosure = onConnectClosure.clone ()
                clonedClosure.delegate = handler
                clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
                cloned.onConnectClosure = clonedClosure
            }

            if(onDisconnectClosure) {
                clonedClosure = onDisconnectClosure.clone ()
                clonedClosure.delegate = handler
                clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
                cloned.onDisconnectClosure = clonedClosure
            }

            cloned
        }
    }

    private static class GrettyWebSocketHandlerAroundClosure extends GrettyWebSocketHandler {
        WrappedClosure wrapper

        GrettyWebSocketHandlerAroundClosure(Closure closure) {
            wrapper = [closure]
        }

        void onMessage(Object message) {
            wrapper.onMessage message
        }

        void onConnect() {
            wrapper.onConnect()
        }

        void onDisconnect() {
            wrapper.onDisconnect()
        }

        GrettyWebSocketHandler clone() {
            GrettyWebSocketHandlerAroundClosure cloned = super.clone ()
            cloned[wrapper: wrapper.clone(cloned)]
        }
    }

    static GrettyWebSocketHandler fromClosure(Closure closure) {
        new GrettyWebSocketHandlerAroundClosure(closure)
    }

    GrettyWebSocketHandler clone() {
        ((GrettyWebSocketHandler)super.clone())[socketPrivate: null]
    }

    private static class Connection extends ExecutingChannel implements ChannelUpstreamHandler {
        protected Channel channel

        private int connectStatus

        protected GrettyWebSocketHandler handler

        void close () {
            channel.close()
        }

        protected void onMessage(Object message) {
            // it might happen that 1st message from other side came earlier than we received CONNECT
            if(message == GrettyWebSocketEvent.CONNECT) {
                if(connectStatus == 0) {
                    connectStatus = 1
                    handler.doOnConnect()
                }
            }
            else {
                if(message == GrettyWebSocketEvent.DISCONNECT) {
                    if(connectStatus == 1) {
                        connectStatus = 2
                        handler.doOnDisconnect()
                    }
                }
                else {
                    if(connectStatus == 0) {
                        connectStatus = 1
                        handler.doOnConnect()
                    }

                    if(connectStatus == 1) {
                        handler.doOnMessage(message)
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

    protected void doOnMessage(Object message) {
        onMessage(message)
    }

    protected void doOnDisconnect() {
        onDisconnect()
    }

    protected void doOnConnect() {
        onConnect()
    }
}
