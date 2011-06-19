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

import org.jboss.netty.util.internal.ConcurrentHashMap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import groovypp.concurrent.CallLaterExecutors
import org.jboss.netty.channel.ChannelFactory

@Typed class GrettyProxy {
    ConcurrentHashMap<Integer,GrettyProxyClient> proxyChannels = [:]

    protected final SocketAddress address

    protected final ChannelFactory channelFactory

    GrettyProxy (SocketAddress address) {
        this.address = address
        if(address instanceof LocalAddress) {
            channelFactory = new DefaultLocalClientChannelFactory()
        }
        else {
            channelFactory = new NioClientSocketChannelFactory(CallLaterExecutors.newCachedThreadPool(), CallLaterExecutors.newCachedThreadPool())
        }
    }

    void handle(GrettyHttpRequest request, GrettyHttpResponse response) {
        def id = response.channelId
        GrettyProxyClient client = proxyChannels[id]
        if(!client) {
            client = [this, id]
            proxyChannels[id] = client
            client.connect ()
        }

        client.proxyRequest(request, response)
    }

    void stop () {
        for(GrettyProxyClient p in proxyChannels.values())
            p.close ()
        channelFactory.releaseExternalResources()
    }

    void onDisconnect(GrettyProxyClient client) {
        proxyChannels.remove(client.id)
    }
}
