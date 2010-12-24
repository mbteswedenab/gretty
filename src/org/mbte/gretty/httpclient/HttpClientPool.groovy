/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.gretty.httpclient

import groovypp.channels.ExecutingChannel
import org.jboss.netty.channel.ChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import groovypp.concurrent.ResourcePool
import java.util.concurrent.Executor

@Typed abstract class HttpClientPool extends ResourcePool<GrettyClient> {
    SocketAddress remoteAddress

    ChannelFactory channelFactory

    int clientsNumber = Runtime.runtime.availableProcessors() * 100

    int maxClientsConnectingConcurrently = 50

    protected volatile int connectingClients

    protected volatile int connectedClients

    Iterable<GrettyClient> initResources() {
        assert remoteAddress

        if(!executor)
            executor = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors() * 4)

        if(!channelFactory) {
            if(remoteAddress instanceof LocalAddress) {
                channelFactory = new DefaultLocalClientChannelFactory()
            }
            else {
                channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
            }
        }

        executor.execute {
            attemptToConnect()
        }

        []
    }

    private void attemptToConnect () {
        def connecting = connectingClients.incrementAndGet()
        if(connecting >= maxClientsConnectingConcurrently || connecting + connectedClients > clientsNumber) {
            connectingClients.decrementAndGet()
            return
        }

        GrettyClient httpClient = [remoteAddress, channelFactory]
        httpClient.adapter = [
            onConnect: {
                connectingClients.decrementAndGet ()
                connectedClients.incrementAndGet()
                add(httpClient)
                executor.execute {
                    attemptToConnect()
                }
            },

            onConnectFailed: { cause ->
                connectingClients.decrementAndGet ()
                connectedClients.incrementAndGet () // onDisconnect will decrease
                executor.execute {
                    attemptToConnect()
                }
            },

            onDisconnect: {
                connectedClients.decrementAndGet ()
                executor.execute {
                    attemptToConnect()
                }
            }
        ]
        httpClient.connect()

        executor.execute {
            attemptToConnect()
        }
    }

    boolean isResourceAlive(GrettyClient resource) {
        resource.connected
    }

    int getConnectingClients () { connectingClients }

    int getConnectedClients () { connectedClients }
}
