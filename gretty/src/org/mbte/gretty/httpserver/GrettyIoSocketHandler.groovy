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



package org.mbte.gretty.httpserver

@Typed abstract class GrettyIoSocketHandler<S,P> extends GrettyWebSocketHandler<S,P> {
    protected static Timer timer = []

    protected UUID sessionId

    protected void doOnMessage(Object message) {
        if(message) {
            def messages = SocketIO.decode(message.toString())
            for(msg in messages) {
                if(msg instanceof String && ((String)msg).startsWith("~h~"))
                    continue
                onMessage msg
            }
        }
    }

    protected void doOnDisconnect() {
        super.doOnDisconnect()
    }

    protected void doOnConnect() {
        sessionId = UUID.randomUUID()
        send sessionId.toString()

        timer.scheduleAtFixedRate ({
            send "~h~heartbeat"
        } as TimerTask, 0, 13500)

        super.doOnConnect()
    }

    protected void send(Object object) {
        super.send(SocketIO.encode(object))
    }

    protected void broadcast(Object object) {
        super.broadcast(SocketIO.encode(object))
    }

    private static class GrettyIoSocketHandlerAroundClosure extends GrettyIoSocketHandler {
        GrettyWebSocketHandler.WrappedClosure wrapper

        GrettyIoSocketHandlerAroundClosure(Closure closure) {
            wrapper = [closure]
        }

        void onMessage(Object message) {
            wrapper.onMessage message
        }

        void onConnect() {
            wrapper.onConnect()
        }

        void onDisconnect() {
            wrapper.onDisconnect()
        }

        GrettyIoSocketHandler clone() {
            GrettyIoSocketHandlerAroundClosure cloned = super.clone ()
            cloned[wrapper: wrapper.clone(cloned)]
        }
    }

    static GrettyIoSocketHandler fromClosure(Closure closure) {
        new GrettyIoSocketHandlerAroundClosure(closure)
    }

    GrettyIoSocketHandler clone() {
        GrettyIoSocketHandler cloned = super.clone()
        cloned[sessionId: null]
    }
}
