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
