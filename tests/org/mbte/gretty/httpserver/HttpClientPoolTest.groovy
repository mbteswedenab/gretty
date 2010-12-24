/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.gretty.httpserver

import org.mbte.gretty.httpclient.GrettyClient
import org.mbte.gretty.httpclient.HttpClientPool
import groovypp.concurrent.ResourcePool
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Typed class HttpClientPoolTest extends GroovyTestCase {
    private GrettyServer server

    protected void setUp() {
        server = [
    localAddress: new InetSocketAddress(InetAddress.localHost.hostName, 8080),

    webContexts: [
        "/ping" : [
            default: {
                response.html = """
<html>
    <head>
        <title>Ping page</title>
    </head>
    <body>
        Hello, World!
    </body>
</html>
                """
            }
        ]
    ]
]

        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testMe () {
        HttpClientPool load = [
            remoteAddress: server.localAddress,

            maxClientsConnectingConcurrently: 10,

            clientsNumber: 100
        ]

        def cdl = new CountDownLatch(100*10)

        def printStat = { String reason ->
            synchronized(cdl) { // does not really matter on what to sync
              println "$reason: $load.connectingClients $load.connectedClients"
            }
        }

        for(i in 0..<100) {
            AtomicInteger iterations = [10]
            load.allocateResource { grettyClient ->
                ResourcePool.Allocate  withClient = this

                if(!grettyClient.connected) {
                    load.releaseResource grettyClient
                    load.allocateResource withClient
                    return
                }

                GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, "/ping"]
                req.setHeader HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE
                try {
                    grettyClient.request(req, load.executor) { responseBindLater ->
                        try {
                            def response = responseBindLater.get()
                            if(response?.status != HttpResponseStatus.OK) {
                                printStat "C$i: response ${response?.status}"

                                load.executor.execute {
                                    withClient(grettyClient)
                                }
                            }
                            else {
                                printStat "C$i: iteration ${iterations.get()} completed ${response.status}"
                                cdl.countDown ()

                                if(iterations.decrementAndGet() > 0) {
                                    load.executor.execute {
                                        withClient(grettyClient)
                                    }
                                }
                                else {
                                    printStat "C$i: job completed"
                                    // we don't disconnect channel here with purpose
                                    // otherwise new one will appear in the pool
                                }
                            }
                        }
                        catch(e) {
                            printStat "C$i: $e"
                            // we need to retry with new client
                            load.releaseResource grettyClient
                            load.allocateResource withClient
                        }
                    }
                }
                catch(e) {
                    // we need to retry with new client
                    load.releaseResource grettyClient
                    load.allocateResource withClient
                }
            }
        }

        cdl.await()
        println "DONE"
    }
}
