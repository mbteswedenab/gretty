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
import org.mbte.gretty.httpclient.HttpRequestHelper

@Typed class ProxyTest extends GroovyTestCase implements HttpRequestHelper {

    private GrettyServer server, proxy
    private GrettyProxy myProxy

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server_with_proxy"),

            webContexts: [
                "/" : [
                    defaultHandler: {
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

            defaultHandler: {
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

                        websocket("/ws"){ event ->
                            switch(event) {
                                case String:
                                    println "-- $event"
                                    send(event.toUpperCase())
                                break

                                case GrettyWebSocketEvent.CONNECT:
                                    send("Welcome!")
                                break

                                case  GrettyWebSocketEvent.DISCONNECT:
                                break
                            }
                        }
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

        CountDownLatch cdl = [11]

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
