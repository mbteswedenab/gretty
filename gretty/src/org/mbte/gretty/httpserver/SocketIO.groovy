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

import org.mbte.gretty.JacksonCategory
import org.jboss.netty.channel.ChannelHandlerContext

@Typed class SocketIO {
    static String encode (def obj) {
        if(!(obj instanceof String)) {
            obj = "~j~${JacksonCategory.toJsonString(obj)}"
        }
        "~m~${obj.length()}~m~$obj"
    }

    static List decode(String data) {
        def messages = []
        def len = data.length()
        String bytes

        for (int i = 0; i < len; ++i) {
          if (i == data.indexOf('~m~', i)) {
            i += 3
            bytes = data.substring(i, data.indexOf('~m~', i));
            i += bytes.length() + 3;

            int mlen
            try {
                mlen = Integer.parseInt(bytes, 10);
            }
            catch(e) {
                continue
            }

            def msg = data.substring(i, i + mlen)
            if(msg.startsWith("~j~")) {
                msg = JacksonCategory.fromJson(Map, msg.substring(3))
            }
            messages << msg
            i += mlen
          }
        }
        messages
    }

    static class GrettyWebSocketHandlerWrapper extends GrettyWebSocketHandler {
        private GrettyWebSocketHandler handler

        GrettyWebSocketHandlerWrapper(GrettyWebSocketHandler handler) {
            this.handler = handler
        }

        protected void initConnection(ChannelHandlerContext ctx, GrettyHttpResponse response) {
            handler.allConnected = allConnected
            handler.server = server
            handler.socketPath = socketPath
            handler.socket = socket = createSocket(ctx.channel)
            handler.initConnection(ctx, response)
        }

        void onMessage(Object message) {
            handler.onMessage message
        }

        protected void onConnect() {
            handler.onConnect()
        }

        protected void onDisconnect() {
            handler.onDisconnect()
        }

        GrettyWebSocketHandlerWrapper clone() {
            ((GrettyWebSocketHandlerWrapper)super.clone())[handler: handler]
        }
    }

    static GrettyWebSocketHandlerWrapper wrap(GrettyWebSocketHandler handler) {
        [handler]
    }
}
