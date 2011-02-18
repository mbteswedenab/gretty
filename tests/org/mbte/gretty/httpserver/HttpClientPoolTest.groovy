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
        def random = new Random ()
        server = [
    localAddress: new InetSocketAddress(InetAddress.localHost.hostName, 8080),

    webContexts: [
        "/ping" : [
            default: {
                if(random.nextInt(1000) < 200) {
                    response.channel.close ()
                    return
                }
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

            clientsNumber: 250
        ]

        def cdl = new CountDownLatch(500*100)

        def printStat = { String reason ->
            synchronized(cdl) { // does not really matter on what to sync
              println "$reason: $load.connectingClients $load.connectedClients"
            }
        }

        AtomicInteger jobsCompleted = [0]
        for(i in 0..<500) {
            AtomicInteger iterations = [100]

            load.allocateResource { grettyClient ->
                ResourcePool.Allocate  withClient = this

                GrettyHttpRequest req = [HttpVersion.HTTP_1_0, HttpMethod.GET, "/ping"]
                req.setHeader HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE
                try {
                    grettyClient.request(req, load.executor) { responseBindLater ->
                        try {
                            def response = responseBindLater.get()
                            if(response?.status != HttpResponseStatus.OK) {
                                printStat "C$i: response ${response?.status}"

                                load.repeat grettyClient, withClient
                            }
                            else {
                                printStat "C$i: iteration ${iterations.get()} completed ${response.status}"
                                cdl.countDown ()

                                if(iterations.decrementAndGet() > 0) {
                                    load.repeat grettyClient, withClient
                                }
                                else {
                                    printStat "C$i: job completed ${jobsCompleted.incrementAndGet()}"
                                    load.releaseResource grettyClient
                                }
                            }
                        }
                        catch(e) {
                            printStat "C$i: $e"
                            // we need to retry with new client
                            load.repeat grettyClient, withClient
                        }
                    }
                }
                catch(e) {
                    // we need to retry with new client
                    load.repeat grettyClient, withClient
                }
            }
        }

        cdl.await()
        println "DONE"
    }
}
