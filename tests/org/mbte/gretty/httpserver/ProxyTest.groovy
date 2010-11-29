package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.GrettyWebsocketClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Typed class ProxyTest extends GroovyTestCase implements HttpRequestHelper {

    private GrettyServer server, proxy
    private GrettyProxy myProxy

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server_with_proxy"),

            webContexts: [
                "/" : [
                    default: {
                        response.addHeader "Default", "true"
                        for(p in request.parameters.entrySet())
                            response.addHeader(p.key, p.value.toString())
                        response.text = "default: path: ${request.path}"
                    }
                ]
            ]
        ]
        server.start()

        myProxy = [new LocalAddress("test_server_with_proxy")]
        proxy = [
            localAddress: new LocalAddress("test_server"),

            default: {
                myProxy.handle(request, response)
            },

            webContexts: [
                "/" : [
                    public: {
                        get("/data/:mapId/set/:objectId") {
                            response.addHeader("mapId", it.mapId)
                            response.addHeader("objectId",  it.objectId)
                        }

                        get("/data/") {}

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
        proxy.start()
    }

    protected void tearDown() {
        myProxy.stop()
        proxy.stop ()
        server.stop ()
    }

    void testDefault () {
        doTest("/data?msg=12&value=33") { response ->
            def bytes = new byte [response.content.readableBytes()]
            response.content.getBytes(0, bytes)
            def text = new String(bytes, "UTF-8")

            assertEquals "default: path: /data", text
            assertEquals "true", response.getHeader("Default")
            assertEquals "[12]", response.getHeader("msg")
            assertEquals "[33]", response.getHeader("value")
        }
    }

    void testMatch() {
        doTest("/data/abracadabra/set/245") { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
        }
    }

    void testNoMatch() {
        doTest("/data") { response ->
            assertNull response.getHeader("mapId")
            assertNull response.getHeader("objectId")
        }
    }

    void testWebSocket () {
        def timer = new Timer ()
        AtomicInteger counter = [0]

        CountDownLatch cdl = [21]

        GrettyWebsocketClient client = [
            'super' : [new LocalAddress("test_server"), "/ws"],

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
