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

import org.jboss.netty.channel.ChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import groovypp.channels.MessageChannel
import groovypp.channels.ExecutingChannel
import java.util.concurrent.Executor
import groovypp.concurrent.BindLater
import org.mbte.gretty.httpserver.GrettyHttpResponse
import org.jboss.netty.handler.codec.http.HttpRequest

@Typed abstract class LoadGenerator {
    SocketAddress remoteAddress

    ChannelFactory channelFactory

    int clientsNumber = Runtime.runtime.availableProcessors() * 100

    int maxClientsConnectingConcurrently = 50

    volatile int connectingClients

    volatile int connectedClients

    volatile int ids

    Executor executor

    void start () {
        assert remoteAddress

        if(!executor)
            executor = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())

        if(!channelFactory) {
            if(remoteAddress instanceof LocalAddress) {
                channelFactory = new DefaultLocalClientChannelFactory()
            }
            else {
                channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
            }
        }

        attemptToConnect ()
    }

    private void attemptToConnect () {
        for(;;) {
            def connected  = connectedClients
            def connecting = connectingClients
            if(connected + connecting >= clientsNumber)
                break

            if(connecting < maxClientsConnectingConcurrently) {
                connectingClients.incrementAndGet()

                GrettyClient httpClient = [remoteAddress, channelFactory]

                LoadClient loadClient = createClient()
                loadClient.client = httpClient
                loadClient.executor = executor

                httpClient.adapter = [
                    onConnect: {
                        loadClient.id = ids.incrementAndGet ()
                        loadClient.schedule {
                            loadClient.onConnect()
                        }
                        connectedClients.incrementAndGet ()
                        connectingClients.decrementAndGet ()
                        attemptToConnect()
                    },

                    onConnectFailed: { cause ->
                        loadClient.schedule {
                            loadClient.onConnectFailed(cause)
                        }
                        connectingClients.decrementAndGet ()
                        attemptToConnect()
                    },

                    onDisconnect: {
                        loadClient.schedule {
                            loadClient.onDisconnect()
                        }
                        attemptToConnect()
                    },
                ]
                httpClient.connect()
            }
        }
    }

    abstract protected LoadClient createClient()

    static class LoadClient extends ExecutingChannel implements org.mbte.gretty.httpclient.AbstractHttpClientHandler<GrettyClient> {
        int id

        void request(HttpRequest request, Function1<GrettyHttpResponse,?> action) {
            client.request(request){ responseBindLater ->
                schedule {
                    action(responseBindLater.get())
                }
            }
        }
    }
}
