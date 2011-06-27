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

package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress

import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion

import org.mbte.gretty.httpclient.HttpRequestHelper

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

//    void testLoad() {
//        def clientsNumber = 1000
//
//        def interationPerClient = 10
//
//        def cdl = new CountDownLatch(clientsNumber * interationPerClient)
//
//        LoadGenerator load = [
//            remoteAddress:new InetSocketAddress("localhost", 8080),
//
//            maxClientsConnectingConcurrently: 1000,
//
//            clientsNumber: clientsNumber,
//
//            printStat: { String reason ->
//                synchronized(cdl) { // does not really matter on what to sync
//                  println "$reason: $connectingClients $connectedClients"
//                }
//            },
//
//            createClient: {
//                printStat('client created')
//                [
//                    iterations: interationPerClient,
//
//                    test: {
//                        schedule {
//                            GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, "/GppGrailsTest/"]
//                            req.setHeader HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE
////                            req.setAuthorization ('Alladin', 'open sesame')
////                            def content = "blah-blah-blah".asChannelBuffer()
////                            req.content = content
////                            req.setHeader(HttpHeaders.Names.CONTENT_LENGTH, content.readableBytes())
//                            request(req) { response ->
//                                if(!response) {
//                                    printStat "null responce"
//                                    while(iterations > 0)
//                                        cdl.countDown()
//                                    return
//                                }
////                                assert response.content.asString() == 'BLAH-BLAH-BLAH'
//
//                                printStat "C$id: job completed"
//                                if(iterations > 0) {
//                                    iterations--
//                                    test()
//                                }
//                                else {
//                                    printStat("C$id: client finished")
//                                    client.close()
//                                }
//                                cdl.countDown ()
//                            }
//                        }
//                    },
//
//                    onConnect: {
//                        printStat("C$id: client connected")
//                        test ()
//                    },
//
//                    onDisconnect: {
//                        printStat("C$id: client disconnected")
//                    }
//                ]
//            }
//        ]
//
//        load.start()
//        cdl.await()
//
//        println "COMPLETED"
//
////        assert load.connectedClients == clientsNumber
////        assert load.connectingClients == 0
////        assert load.ids == clientsNumber
////        load.stop ()
//    }
//
//    void testLoadWithPool() {
//        def clientsNumber = 5
//
//        def iterationPerClient = 100
//
//        def totalIterations = clientsNumber * iterationPerClient
//
//        def cdl = new CountDownLatch(totalIterations)
//
//        HttpClientPool load = [
//            remoteAddress:new InetSocketAddress("lucy.bindows.net", 80),
//
//            maxClientsConnectingConcurrently: 2,
//
//            clientsNumber: clientsNumber,
//
//            isResourceAlive: { resource ->
//                ((GrettyClient)resource).isConnected ()
//            }
//
//        ]
//
//        def printStat = { String reason ->
//            synchronized(cdl) { // does not really matter on what to sync
//              println "$reason: $load.connectingClients $load.connectedClients"
//            }
//        }
//
//        def jobCount = new AtomicInteger()
//
//        for(i in 0..<totalIterations) {
//            load.allocateResource { grettyClient ->
//                def operation = this
//
//                GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, "/lucy/login/auth"]
//                req.setHeader HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE
//                try {
//                    grettyClient.request(req, load.executor) { responseBindLater ->
//                        def that = this
//                        try {
//                            def response = responseBindLater.get()
//                            if(!response) {
//                                printStat "C$i: null response"
//                                load.allocateResource operation
//                            }
//                            else {
//                                printStat "C$i: job completed ${jobCount.incrementAndGet()}"
//                                cdl.countDown ()
//                            }
//                        }
//                        catch(e) {
//                            printStat "C$i: exception"
//                            load.allocateResource operation
//                        }
//                        finally {
//                            load.releaseResource(grettyClient)
//                        }
//                    }
//                }
//                catch(e) {
//                    load.releaseResource(grettyClient)
//                    load.allocateResource operation
//                }
//            }
//        }
//
//        cdl.await()
//
//        println "COMPLETED"
//
////        assert load.connectedClients == clientsNumber
////        assert load.connectingClients == 0
////        assert load.ids == clientsNumber
////        load.stop ()
//    }
}
