package org.mbte.gretty.httpserver

import java.util.concurrent.CountDownLatch
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.mbte.gretty.httpclient.LoadGenerator

@Typed(TypePolicy.MIXED) class RestTest extends GroovyTestCase implements HttpRequestHelper {

    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        rest("/data/:mapId/:objectId") {
                            get {
                                response.addHeader("mapId", it.mapId)
                                response.addHeader("objectId",  it.objectId)
                                response.addHeader("method",  "get")
                                response.text = "OK"
                                response.status = HttpResponseStatus.OK
                            }

                            post {
                                response.addHeader("mapId", it.mapId)
                                response.addHeader("objectId",  it.objectId)
                                response.addHeader("method",  "post")
                                response.text = request.content.asString().toUpperCase()
                                response.status = HttpResponseStatus.OK
                            }
                        }
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testGet() {
        GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/data/abracadabra/245"]
        doTest(req) { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
            assertEquals "get", response.getHeader("method")
        }
    }

    void testPost() {
        GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/data/abracadabra/245"]
        doTest(req) { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
            assertEquals "post", response.getHeader("method")
        }
    }

    void testLoad() {
        def clientsNumber = Runtime.runtime.availableProcessors() * 100

        def interationPerClient = 5

        def cdl = new CountDownLatch(clientsNumber * interationPerClient)

        LoadGenerator load = [
                remoteAddress:new LocalAddress("test_server"),

                clientsNumber: clientsNumber,

                printStat: { reason ->
                    synchronized(cdl) { // does not matter on what to sync
                      println "$reason: $connectingClients $connectedClients"
                    }
                },

                createClient: {
                    printStat('client created')
                    [
                        iterations: interationPerClient,

                        test: {
                            schedule {
                                GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/data/abracadabra/245"]
                                req.setAuthorization ('Alladin', 'open sesame')
                                def content = "blah-blah-blah".asChannelBuffer()
                                req.content = content
                                req.setHeader(HttpHeaders.Names.CONTENT_LENGTH, content.readableBytes())
                                request(req) { response ->
                                    assertEquals "abracadabra", response.getHeader("mapId")
                                    assertEquals "245", response.getHeader("objectId")
                                    assertEquals "post", response.getHeader("method")

                                    assert response.content.asString() == 'BLAH-BLAH-BLAH'

                                    printStat "C$id: job completed"
                                    if(iterations > 0) {
                                        iterations--
                                        test()
                                    }
                                    else {
                                        printStat("C$id: client finished")
                                        client.close()
                                    }
                                    cdl.countDown ()
                                }
                            }
                        },

                        onConnect: {
                            printStat("C$id: client connected")
                            test ()
                        },

                        onDisconnect: {
                            printStat("C$id: client disconnected")
                        }
                    ]
                }
        ]

        load.start()
        cdl.await()

        assert load.connectedClients == clientsNumber
        assert load.connectingClients == 0
        assert load.ids == clientsNumber
//        load.stop ()
    }
}
