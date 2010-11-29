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

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ChannelUpstreamHandler
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelState
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame

import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import groovypp.concurrent.FList

@Typed abstract class GrettyWebSocket {
    private volatile FList<GrettyWebSocketListener> listeners = FList.emptyList

    void send(Object object) {
        write(object.toString())
    }

    protected abstract void write(String object)

    abstract void close ()

    void addListener(GrettyWebSocketListener listener) {
        for(;;) {
            def ll = listeners
            if (listeners.compareAndSet(ll, ll + listener)) {
                break
            }
        }
    }

    protected void notifyDisconnect () {
        for(l in listeners) {
            try {
                l.onDisconnect()
            }
            catch(e) { //
            }
        }
    }

    protected void notifyMessage (String message) {
        for(l in listeners) {
            try {
                l.onMessage(message)
            }
            catch(e) { //
            }
        }
    }

    static class Channeled extends GrettyWebSocket implements ChannelUpstreamHandler {
        private final Channel channel

        Channeled(Channel channel) {
            this.channel = channel
        }

        void write(String object) {
            channel.write(new DefaultWebSocketFrame(object))
        }

        void close () {
            channel.close()
        }

        void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {
            switch(e) {
                case MessageEvent:
                    WebSocketFrame frame = e.message
                    notifyMessage(frame.textData)
                break

                case ChannelStateEvent:
                    switch(e.state) {
                        case ChannelState.CONNECTED:
                            if(!e.value)
                                notifyDisconnect()
                        break
                    }
                break
            }
        }

        boolean equals(o) {
            if (this.is(o)) return true;

            if (getClass() != o.class) return false;

            Channeled channeled = (Channeled) o;

            if (channel != channeled.channel) return false;

            return true;
        }

        int hashCode() {
            return (channel != null ? channel.hashCode() : 0);
        }
    }
}
