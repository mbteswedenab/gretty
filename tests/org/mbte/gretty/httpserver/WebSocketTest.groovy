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

            webContexts: [
                "/" : [
                    public: {
                        websocket("/ws",[
                            onMessage: { msg ->
                                println "-- $msg"
                                socket.send(msg.toUpperCase())
                            },

                            onConnect: {
                                socket.send("Welcome!")
                            }
                        ])
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testDefault () {
        def timer = new Timer ()
        AtomicInteger counter = [0]

        CountDownLatch cdl = [21]

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
