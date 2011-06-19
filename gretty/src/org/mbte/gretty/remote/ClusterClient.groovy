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

import org.mbte.gretty.remote.RemoteConnectionHandler
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.channel.ChannelPipeline
import org.mbte.gretty.remote.ClusterNode
import org.mbte.gretty.AbstractClient

class ClusterClient extends AbstractClient {
    final ClusterNode clusterNode
    final UUID remoteId

    ClusterClient(SocketAddress remoteAddress, ClusterNode clusterNode, UUID remoteId) {
        super(remoteAddress)
        this.clusterNode = clusterNode
        this.remoteId    = remoteId
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("encoder", new ObjectEncoder())
        pipeline.addLast("decoder", new ObjectDecoder())
        pipeline.addLast("remoteConnectionHandler", new RemoteConnectionHandler(clusterNode, remoteId))
    }
}
