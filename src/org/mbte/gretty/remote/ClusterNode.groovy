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

import groovypp.channels.MessageChannel
import groovypp.channels.MultiplexorChannel
import org.mbte.gretty.remote.inet.InetDiscoveryInfo
import org.mbte.gretty.remote.inet.MulticastChannel

/**
 * Local node in the cluster.
 */
@Typed final class ClusterNode extends SupervisedChannel {
    private InetAddress multicastGroup
    private int         multicastPort

    List<SocketAddress> seedNodes = []

    SocketAddress       address = new InetSocketAddress(InetAddress.getLocalHost(), findFreePort())

    /**
     * Unique id of this node over cluster
     */
    final UUID id = UUID.randomUUID()

    private volatile long nextObjectId

    MessageChannel mainActor

    MultiplexorChannel<CommunicationEvent> communicationEvents = []

    private ClusterServer server

    private final ClientConnector clientConnector = []

    protected final Timer timer = []

    protected final long allocateObjectId () {
        nextObjectId.incrementAndGet ()
    }

    void setMainActor(MessageChannel actor) {
        mainActor = actor.async(executor)
    }

    protected void doStartup() {
        super.doStartup();

        server = [this]
        server.start ()

        startupChild(clientConnector)

        if (address instanceof InetSocketAddress && multicastGroup && multicastPort) {
            startupChild (new MulticastChannel.Sender([
                  multicastGroup: multicastGroup,
                  multicastPort:  multicastPort,
                  dataToTransmit: InetDiscoveryInfo.toBytes(id, (InetSocketAddress)address)
            ]))
        }
    }

    protected void doShutdown() {
        server.stop ()

        super.doShutdown()
    }

    static class CommunicationEvent {
        static class TryingConnect extends CommunicationEvent{
            UUID uuid
            String address

            String toString () {
                "trying to connect to $uuid @ $address"
            }
        }

        static class Connected extends CommunicationEvent{
            RemoteClusterNode remoteNode

            String toString () {
                "connected to ${remoteNode.remoteId}"
            }
        }

        static class Disconnected extends CommunicationEvent{
            UUID remoteId

            String toString () {
                "disconnected from $remoteId"
            }
        }
    }

    InetAddress getMulticastGroup () {
        multicastGroup
    }

    void setMulticastGroup(InetAddress mg) {
        multicastGroup = mg
        if(!multicastPort)
            multicastPort = 4238
    }

    int getMulticastPort() {
        multicastPort
    }

    void setMulticastPort(int port) {
        multicastPort = port
        if(!multicastGroup)
            multicastGroup = InetAddress.getByAddress(230,0,0,239)
    }

    static int findFreePort() {
        def server = new ServerSocket(0)
        def port = server.getLocalPort()
        server.close()
        port
    }
}
