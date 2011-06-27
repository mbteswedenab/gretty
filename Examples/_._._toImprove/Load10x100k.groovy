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

import java.nio.ByteBuffer
import groovypp.channels.MultiplexorChannel
import org.mbte.gretty.httpserver.GrettyServer
import org.jboss.netty.logging.InternalLogLevel

class Data {
    static Random random = []

    static Thread update = [
        run: {
            while(true) {
                def start = System.currentTimeMillis()
                def bytes = ByteBuffer.allocate(32)
                for(i in 0..<8)
                    bytes.putInt(random.nextInt())
                multi << bytes
                def end = System.currentTimeMillis() - start
                if(end < 40) {
                    Thread.sleep(40-end)
                }
            }
        }
    ]

    static MultiplexorChannel multi = []
}

Data.update.start ()

GrettyServer server = [
    logLevel: InternalLogLevel.DEBUG,

    webContexts: [
        "/" : [
            public: {
                websocket("/ws",[
                    onMessage: { msg ->
                    },

                    onConnect: {
                        Data.multi.subscribe { msg ->
                            socket.send msg
                        }
                    }
                ])
            },
        ],
    ]
]
server.start()
