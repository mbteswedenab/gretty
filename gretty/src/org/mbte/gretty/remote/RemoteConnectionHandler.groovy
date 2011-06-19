/*
 * Copyright 2009-2011 MBTE Sweden AB.
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

import org.jboss.netty.channel.*

@Typed class RemoteConnectionHandler extends SimpleChannelHandler {
    protected final    ClusterNode       clusterNode
    protected volatile Channel           channel

    protected volatile RemoteClusterNode remoteNode

    private final TimerTask sendIdentityTask

    RemoteConnectionHandler(ClusterNode clusterNode, UUID remoteId = null) {
        this.clusterNode = clusterNode

//        if(remoteId)
//            remoteNode = new RemoteClusterNode(this, remoteId)

        sendIdentityTask = {
            if (channel.connected)
                channel.write(new RemoteMessage.Identity(senderNodeId:clusterNode.id))
            else {
                cancel()
            }
        }
    }

    void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if(channel.compareAndSet(null, ctx.channel)) {
            // first time
        }
        super.handleUpstream(ctx, e)
    }

    void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        sendIdentityTask.run ()
        clusterNode.timer.scheduleAtFixedRate(sendIdentityTask, 0L, 1000L)
        super.channelConnected(ctx, e)
    }

    void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
//        System.err.println "channel closed ${e.channel}"
        sendIdentityTask.cancel()
        if(remoteNode)
            clusterNode.communicationEvents << new ClusterNode.CommunicationEvent.Disconnected(remoteId:remoteNode.remoteId)
        super.channelClosed(ctx, e)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        RemoteMessage msg = e.message
        if (!remoteNode) {
            if (msg instanceof RemoteMessage.Identity) {
                remoteNode = new RemoteClusterNode(this, msg.senderNodeId)
                clusterNode.communicationEvents << new ClusterNode.CommunicationEvent.Connected(remoteNode:remoteNode)
            }
            else {
                throw new RuntimeException("protocol error: $msg.senderNodeId ${msg.class}")
            }
        }
        else {
            switch(msg) {
                case RemoteClusterNode.ToMainActor:
                    clusterNode.mainActor?.post(msg.payLoad)
                    break;
            }
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.cause.printStackTrace()
        e.channel.close()

        sendIdentityTask.cancel()
        if(remoteNode)
            clusterNode.communicationEvents << new ClusterNode.CommunicationEvent.Disconnected(remoteId:remoteNode.remoteId)
    }

    void send(RemoteMessage msg) {
        msg.senderNodeId = clusterNode.id
        channel.write(msg)
    }
}
