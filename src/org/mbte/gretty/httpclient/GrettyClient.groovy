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

package org.mbte.gretty.httpclient

import org.jboss.netty.handler.codec.http.HttpResponse

import groovypp.concurrent.BindLater
import org.jboss.netty.handler.codec.http.HttpRequest

import org.jboss.netty.channel.ChannelHandlerContext

import org.jboss.netty.channel.MessageEvent

import org.mbte.gretty.httpserver.GrettyHttpResponse
import org.jboss.netty.channel.ChannelFactory

@Typed class GrettyClient extends AbstractHttpClient {

    private volatile BindLater<HttpResponse> pendingRequest

    GrettyClient(SocketAddress remoteAddress, ChannelFactory factory = null) {
        super(remoteAddress, factory)
    }

    BindLater<HttpResponse> request(HttpRequest request) {
        def later = new BindLater()
        assert pendingRequest.compareAndSet(null, later)
        channel.write(request)
        later
    }

    void request(HttpRequest request, BindLater.Listener<GrettyHttpResponse> action) {
        assert pendingRequest.compareAndSet(null, new BindLater().whenBound(action))
        channel.write(request)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        pendingRequest.getAndSet(null).set((GrettyHttpResponse)e.message)
    }
}
