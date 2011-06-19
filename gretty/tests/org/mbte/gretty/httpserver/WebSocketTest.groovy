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

import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.GrettyWebsocketClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Typed class WebSocketTest extends GroovyTestCase {
    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server2"),

            public: {
                websocket("/ws"){ event ->
                    onConnect: {
                        send "Welcome"
                    }

                    onDisconnect: {
                    }

                    switch(event) {
                        case String:
                            println "-- $event"
                            broadcast(event.toUpperCase())
                        break
                    }
                }
            }
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testDefault () {
        def timer = new Timer ()
        AtomicInteger counter = [0]

        CountDownLatch cdl = [11]

        GrettyWebsocketClient client = [
            'super' : [new LocalAddress("test_server2"), "/ws"],

            onMessage: { msg ->
                println "<< $msg"
                cdl.countDown()
            },

            onWebSocketConnect: {
                timer.scheduleAtFixedRate ({
                    def string = "Hello, world! $counter"
                    send(string)
                    println ">> $string"
                    cdl.countDown()
                    if(counter.incrementAndGet() == 10) {
                        cancel ()
                    }
                }, 10, 50)
            }
        ]

        client.connect ()

        cdl.await()
        client.disconnect()
        println "done"

        timer.cancel()
    }

    void testMulti () {
        Timer timer = []
        AtomicInteger counter = []
        for(i in 0..<10) {
            CountDownLatch cdl = [1]
            GrettyWebsocketClient client = [
                'super' : [new LocalAddress("test_server2"), "/ws"],

                onMessage: { msg ->
                    println "<< $msg"
                    cdl.countDown()
                },

                onWebSocketConnect: {
                    timer.scheduleAtFixedRate ({
                        def string = "Hello, world! $counter"
                        send(string)
                        println ">> $string"
                        cdl.countDown()
                        if(counter.incrementAndGet() == 10) {
                            cancel ()
                        }
                    }, 10, 50)
                }
            ]

            client.connect ()

            cdl.await()
            client.disconnect()
            println "done"
        }
    }
}
