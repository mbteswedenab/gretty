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

package org.mbte.gretty.remote

import groovypp.channels.SupervisedChannel

import org.mbte.gretty.remote.inet.InetDiscoveryInfo
import org.mbte.gretty.remote.inet.MulticastChannel

import org.jboss.netty.channel.group.DefaultChannelGroup

@Typed class ClientConnector extends SupervisedChannel<ClusterNode> {

    private final DefaultChannelGroup allConnected = []

    public void doStartup() {
        super.doStartup()
        if (owner.multicastGroup && owner.multicastPort) {
            startupChild(new MulticastChannel.Receiver([
                  multicastGroup: owner.multicastGroup,
                  multicastPort: owner.multicastPort,
            ]))
        }
    }

    public void doShutdown() {
        clients.clear ()
        allConnected.close()

        super.doShutdown()
    }

    protected void doOnMessage(Object msg) {
        switch(msg) {
            case InetDiscoveryInfo:
                if (!stopped() && msg.clusterId > owner.id) {
                    synchronized(clients) {
                        if(!clients.containsKey(msg.clusterId)) {
                            owner.communicationEvents << new ClusterNode.CommunicationEvent.TryingConnect(uuid:msg.clusterId, address:msg.serverAddress)
                            def client = new ClusterClient(msg.serverAddress, owner, msg.clusterId)
                            clients.put(msg.clusterId, client)
                            client.connect().addListener { future ->
                                def channel = future.channel
                                allConnected.add(channel)
                                channel.closeFuture.addListener { future2 ->
                                    synchronized(clients) {
                                        clients.remove(client.remoteId)
                                    }
                                }
                            }
                        }
                    }
                }
                break;

            default:
                super.doOnMessage(msg)
        }
    }

    private HashMap<UUID,ClusterClient> clients = [:]
}