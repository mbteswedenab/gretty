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

import static org.jboss.netty.handler.codec.http.HttpHeaders.*
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.*

import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.MessageEvent

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpMethod

import java.security.MessageDigest
import org.jboss.netty.buffer.ChannelBuffers

import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder
import org.codehaus.jackson.node.ObjectNode

import org.codehaus.jackson.node.ArrayNode
import org.jboss.netty.buffer.ChannelBufferInputStream
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.logging.InternalLoggerFactory
import org.jboss.netty.logging.InternalLogger
import org.jboss.netty.logging.InternalLogLevel
import org.mbte.gretty.httpserver.GrettyWebSocket.Channeled

@Typed class GrettyAppHandler extends SimpleChannelHandler {
    private static final WEBSOK_OK = new HttpResponseStatus(101,"Web Socket Protocol Handshake")
    private static final MessageDigest MD5 = MessageDigest.getInstance("MD5")

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GrettyAppHandler)

    Map<String,GrettyContext> webContexts
    final GrettyServer server

    GrettyAppHandler(GrettyServer server) {
        this.server = server
        this.webContexts = server.webContexts
    }

    def void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
//        if(logger.isEnabled(InternalLogLevel.DEBUG))
//            logger.debug("${ctx.channel} CONNECTED")
        super.channelConnected(ctx, e)
    }

    def void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
//        if(logger.isEnabled(InternalLogLevel.DEBUG))
//            logger.debug("${ctx.channel} DISCONNECTED")
        super.channelDisconnected(ctx, e)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        def msg = e.message
        switch(msg) {
            case GrettyHttpRequest:
                def req = msg
                server.execute {
                    try {
                        if(UPGRADE.equalsIgnoreCase(req.getHeader(CONNECTION)) && WEBSOCKET.equalsIgnoreCase(req.getHeader(UPGRADE))) {
                            handleWebSocketRequest(ctx, e)
                        }
                        else {
                            def pseudo = req.getHeader("Pseudo-WebSocket")
                            if(pseudo)
                                handlePseudoWebSocket(ctx, e)
                            else
                                handleHttpRequest(req, e)
                        }
                    }
                    catch(throwable) {
                        //
                    }
                }
            break

            default:
                super.messageReceived(ctx, e)
        }
    }

    private ArrayNode fromJson(MessageEvent e) {
        GrettyHttpRequest request = e.message
        ChannelBufferInputStream is = [request.content]
        ArrayNode res
        try {
            ObjectMapper mapper = []
            res = ((ObjectNode)mapper.readValue(is, JsonNode)).get("messages")

        }
        finally {
            is.close ()
        }
        res
    }

    private void handlePseudoWebSocket(ChannelHandlerContext ctx, MessageEvent e) {
        GrettyHttpRequest request = e.message
        GrettyHttpResponse response = [e.channel,isKeepAlive(request)]
        if (request.method != HttpMethod.POST) {
            e.channel.write(response).addListener(ChannelFutureListener.CLOSE)
        }

        def uri = request.uri
        def context = findContext(uri)
        def webSocket = context?.webSockets?.get((uri.substring(context?.webPath?.length())))
        if (!webSocket) {
            e.channel.write(response).addListener(ChannelFutureListener.CLOSE)
        }
        else {
            def sessionId = request.getHeader("Pseudo-WebSocket")
            if ('null'.equals(sessionId)) {
                def client = server.pseudoWebSocketManager.allocateId(webSocket)
                sessionId = client.sessionId

                response.json = "{\"sessionId\":\"$sessionId\",\"messages\":[]}"
                response.complete()

                client.handler.connect(client, request)
            }
            else {
                def client = server.pseudoWebSocketManager.getClient(sessionId)
                if(!client) {
                    e.channel.write(response).addListener(ChannelFutureListener.CLOSE)
                }
                else {
                    client.onRequest(e.channel, fromJson(e))
                }
            }
        }
    }

    private void handleHttpRequest(GrettyHttpRequest request, MessageEvent e) {
        GrettyHttpResponse response = [e.channel, isKeepAlive(request)]
        response.protocolVersion = request.protocolVersion
        def uri = request.path

        findContext(uri)?.handleHttpRequest(request, response)

        if (!response.async)
            response.complete()
    }

    private GrettyContext findContext(String uri) {
        webContexts.entrySet().find { wc -> uri.startsWith(wc.key) }?.value
    }

    private void handleWebSocketRequest(ChannelHandlerContext ctx, MessageEvent e) {
        GrettyHttpRequest req = e.message
        GrettyHttpResponse response = [e.channel,isKeepAlive(req)]
        if (req.method != HttpMethod.GET) {
            e.channel.write(response.responseBody).addListener(ChannelFutureListener.CLOSE)
        }

        def uri = req.uri
        def context = findContext(uri)
        def webSocket = context?.webSockets?.get((uri.substring(context?.webPath?.length())))
        if (!webSocket) {
            e.channel.write(response).addListener(ChannelFutureListener.CLOSE)
        }
        else {
            webSocket = webSocket.clone ()

            response.addHeader(UPGRADE, WEBSOCKET)
            response.addHeader(CONNECTION, UPGRADE)
            response.status = WEBSOK_OK

            def location = "ws://${req.getHeader(HOST)}${context.webPath}${webSocket.socketPath}"
            if (req.containsHeader(SEC_WEBSOCKET_KEY1) && req.containsHeader(SEC_WEBSOCKET_KEY2)) {
                response.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN))
                response.addHeader(SEC_WEBSOCKET_LOCATION, location)

                def protocol = req.getHeader(SEC_WEBSOCKET_PROTOCOL)
                if (protocol) {
                    response.addHeader(SEC_WEBSOCKET_PROTOCOL, protocol)
                }

                // Calculate the answer of the challenge.
                def key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
                def key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
                def a = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1.replaceAll("[^ ]", "").length());
                def b = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2.replaceAll("[^ ]", "").length());
                def c = req.getContent().readLong();
                def input = ChannelBuffers.buffer(16)
                input.writeInt(a)
                input.writeInt(b)
                input.writeLong(c)
                def output = ChannelBuffers.wrappedBuffer(MD5.digest(input.array()))
                response.setContent(output);
            } else {
                response.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN))
                response.addHeader(WEBSOCKET_LOCATION, location)
                def protocol = req.getHeader(WEBSOCKET_PROTOCOL)
                if (protocol != null) {
                    response.addHeader(WEBSOCKET_PROTOCOL, protocol)
                }
            }

            def p = ctx.channel.pipeline

            p.remove("chunkedWriter")
            p.remove("fileWriter")
            p.replace("http.request.decoder", "wsdecoder", new WebSocketFrameDecoder())

            def channeled = new Channeled(e.channel)
            channeled.addListener(webSocket)
            p.addLast("websocket.handler", channeled)

            e.channel.write(response).addListener {
                p.remove("http.application")
                p.replace("http.request.encoder", "wsencoder", new WebSocketFrameEncoder())

                webSocket.connect(channeled, req)
            }
        }
    }
}