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
import org.mbte.gretty.httpserver.GrettyHttpResponse
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.ChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import groovypp.concurrent.ResourcePool

@Typed abstract class HttpClientPool extends ResourcePool<GrettyClient> {
    SocketAddress remoteAddress

    ChannelFactory channelFactory

    int clientsNumber = Runtime.runtime.availableProcessors() * 100

    int maxClientsConnectingConcurrently = 50

    protected int connectingClients

    protected int connectedClients

    private ExecutingChannel lock = new ExecutingChannel(){}

    Iterable<GrettyClient> initResources() {
        assert remoteAddress

        if(!executor)
            executor = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors() * 4)

        lock.executor = executor

        if(!channelFactory) {
            if(remoteAddress instanceof LocalAddress) {
                channelFactory = new DefaultLocalClientChannelFactory()
            }
            else {
                channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
            }
        }

        lock.schedule {
            attemptToConnect()
        }

        []
    }

    private void attemptToConnect () {
        if(connectedClients + connectingClients >= clientsNumber)
            return

        if(connectingClients < maxClientsConnectingConcurrently) {
            connectingClients++

            GrettyClient httpClient = [remoteAddress, channelFactory]

            httpClient.adapter = [
                onConnect: {
                    lock.schedule {
                        connectedClients++
                        connectingClients--
                        add(httpClient)
                        attemptToConnect()
                    }
                },

                onConnectFailed: { cause ->
                    lock.schedule {
                        connectingClients--
                        connectedClients++  // onDisconnect will decrease
                        attemptToConnect()
                    }
                },

                onDisconnect: {
                    lock.schedule {
                        connectedClients--
                        attemptToConnect()
                    }
                },
            ]
            httpClient.connect { future ->
                lock.schedule {

                }
            }

            lock.schedule {
                attemptToConnect()
            }
        }
    }

    int getConnectingClients () { connectingClients }

    int getConnectedClients () { connectedClients }
}
