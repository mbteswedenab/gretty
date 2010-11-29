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

import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpVersion

import org.codehaus.jackson.node.ObjectNode
import org.codehaus.jackson.node.JsonNodeFactory

import org.jboss.netty.channel.ChannelFuture

import org.codehaus.jackson.node.ArrayNode
import org.jboss.netty.handler.codec.http.HttpResponseStatus

@Typed class PseudoWebSocket extends GrettyWebSocket implements ChannelFutureListener  {
    protected final PseudoWebSocketManager manager
    protected       GrettyWebSocketHandler handler

    String sessionId

    TimerTask timerTask

    private volatile State state

    PseudoWebSocket (PseudoWebSocketManager manager) {
        this.manager = manager
        state = [writeChannel:null, waitingChannel:null]

        timerTask = { doWrite() }
        manager.timer.scheduleAtFixedRate(timerTask, 250, 200)
    }

    private static class State implements Cloneable {
        // channel which is pending uncompleted writes
        Channel          writeChannel

        // channel where next write should go
        Channel          waitingChannel

        // output queue to be sent after a channel become available for write
        groovypp.concurrent.FList<String>    outputQueue = groovypp.concurrent.FList.emptyList

        Object clone () { super.clone() }
    }

    private void doWrite() {
        for(;;) {
            def s = state
            State ns = s.clone()

            if (ns.waitingChannel && !ns.writeChannel && !ns.outputQueue.empty) {
                ns.writeChannel = ns.waitingChannel
                ns.waitingChannel = null

                def toSend = ns.outputQueue
                ns.outputQueue = groovypp.concurrent.FList.emptyList
                if(state.compareAndSet(s, ns)) {
                    post(toSend, ns)
                    break
                }
            }
            else {
                if(state.compareAndSet(s, ns)) {
                    break
                }
            }
        }
    }

    void write(String message) {
        for(;;) {
            def s = state
            State ns = s.clone()
            ns.outputQueue = ns.outputQueue + message
            if(state.compareAndSet(s, ns)) {
                break
            }
        }
    }

    void close() {
    }

    private void onNewChannel(Channel channel) {
        for (;;) {
            def s = state
            State ns = s.clone()

            def oldWaiting = ns.waitingChannel
            ns.waitingChannel = channel

            if (oldWaiting && !ns.writeChannel && !ns.outputQueue.empty) {
                ns.writeChannel = oldWaiting

                def toSend = ns.outputQueue
                ns.outputQueue = groovypp.concurrent.FList.emptyList
                if(state.compareAndSet(s, ns)) {
                    post(toSend, ns)
                    break
                }
            }
            else {
                if(state.compareAndSet(s, ns)) {
                    if(oldWaiting) {
                        oldWaiting.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                    }
                    break
                }
            }
        }
    }

    void onRequest(Channel channel, ArrayNode messages) {
        channel.closeFuture.addListener(this)

        manager.clientPool.execute {
            for(m in messages.elements)
                handler.onMessage(m.valueAsText)
        }

        for (;;) {
            def s = state
            State ns = s.clone()

            def oldWaiting = ns.waitingChannel
            ns.waitingChannel = channel

            if (oldWaiting && !ns.writeChannel && !ns.outputQueue.empty) {
                ns.writeChannel = oldWaiting

                def toSend = ns.outputQueue
                ns.outputQueue = groovypp.concurrent.FList.emptyList

                if(state.compareAndSet(s, ns)) {
                    post(toSend, ns)
                }
            }
            else {
                if(state.compareAndSet(s, ns)) {
                    if(oldWaiting) {
                        oldWaiting.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                    }
                    break
                }
            }
        }
    }

    private def post(groovypp.concurrent.FList<String> toSend, State ns) {
        GrettyHttpResponse res = [null,false]
        def json = new ObjectNode(JsonNodeFactory.instance)
        json.put("sessionId", sessionId)
        def arr = json.putArray("messages")
        for (m in toSend.reverse())
            arr.add(m)
        res.json = json
        ns.writeChannel.write(res).addListener(this)
    }

    private void onChannelClosed(Channel c) {}
    
    private volatile TimerTask timerTask

    private void onWriteCompleted(Channel c) {
        for(;;) {
            def s = state
            assert s.writeChannel == c
            State ns = s.clone()

            ns.writeChannel = null
            if(state.compareAndSet(s, ns)) {
                break;
            }
        }
    }

    void operationComplete(ChannelFuture future) {
        def c = future.channel
        if(future == c.closeFuture) {
            onChannelClosed(c)
        }
        else {
            onWriteCompleted(c)
        }
    }
}
