/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

@Typed package examples

import org.mbte.gretty.*
import org.jboss.netty.buffer.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

println "starting..."

AbstractServer echo = [
        localAddress: new InetSocketAddress(8080),

        messageReceived: { ctx, e ->
            ChannelBuffer cb = e.message
            print new String(cb.array())
            e.channel.write(e.message)
        },

        channelConnected: { ctx, e ->
            println "${e.channel.remoteAddress} connected to server"
        },

        channelDisconnected: { ctx, e ->
            println "${e.channel.remoteAddress} disconnected from server"
        }
]
echo.start()

def executor = Executors.newFixedThreadPool(10)
for(i in 0..<100) {
    executor {
        AbstractClient client = [
            'super': [new InetSocketAddress(8080)],

            onConnect: {
                println "client $i connected"
                write ChannelBuffers.wrappedBuffer("Hello from client $i\n".bytes)
            },

            onDisconnect: {
                println "client $i disconnected"
            },

            messageReceived: { ctx, e ->
                ChannelBuffer cb = e.message
                assert new String(cb.array()) == "Hello from client $i\n"
            },
        ]
        client.connect()
    }
}
