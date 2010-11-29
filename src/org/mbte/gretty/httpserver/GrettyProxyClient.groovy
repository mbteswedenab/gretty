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

import org.mbte.gretty.httpclient.GrettyClient
import org.jboss.netty.channel.ChannelFactory
import groovypp.concurrent.FQueue
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.mbte.gretty.httpclient.AbstractHttpClient
import org.jboss.netty.handler.codec.http.HttpMessage

@Typed class GrettyProxyClient extends AbstractHttpClient {
    static class State {
        FQueue<Pair<GrettyHttpRequest,GrettyHttpResponse>> queue = FQueue.emptyQueue
        boolean connected
    }

    private volatile State state = []

    private final GrettyProxy proxy
    protected final Integer id

    GrettyProxyClient(GrettyProxy proxy, Integer id) {
        super(proxy.address, proxy.channelFactory)
        this.proxy = proxy
        this.id = id
    }

    protected void onConnect() {
        for (;;) {
            def s = state
            if(s.connected)
                throw new IllegalStateException()

            if(state.compareAndSet(s, [queue:s.queue, connected:true])) {
                for(q in s.queue) {
                    write q.first
                }
                break
            }
        }
    }

    protected void onDisconnect() {
        proxy.onDisconnect(this)
    }

    protected void onConnectFailed(Throwable cause) {
        proxy.onDisconnect(this)
    }

    void disconnectAll() {
        def s = state
        if(s.queue.empty || state.compareAndSet(s, [])) {
            for(rr in s.queue) {
                rr.second.close()
            }
        }
    }

    void proxyRequest(GrettyHttpRequest request, GrettyHttpResponse response) {
        response.async = true
        for(;;) {
            def s = state
            if(state.compareAndSet(s, [queue: s.queue + [request, response], connected:s.connected])) {
                println request
                if(s.connected)
                    write(request)
                break
            }
        }
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if(e.message instanceof HttpMessage) {
            for(;;) {
                def s = state
                if(s.queue.empty)
                    break

                def r = s.queue.removeFirst()
                if(state.compareAndSet(s, [queue: r.second, connected:true])) {
                    r.first.second.write(e.message)
                }
            }
        }
        else {
            super.messageReceived(ctx,e)
        }
    }
}
