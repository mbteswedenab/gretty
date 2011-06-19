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

@Typed package org.mbte.gretty.httpserver

import java.lang.management.ManagementFactory

import org.mbte.gretty.httpclient.GrettyClient
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.HttpHeaders
import java.util.logging.*
import org.jboss.netty.logging.InternalLoggerFactory

import java.util.concurrent.Executors

class Performance {
    static void main (String [] args) {
        if(args?.length > 0) {
            startClient(args[0])
        }
        else {
            startServer ()
        }
    }

    static void startServer () {
        def rootLogger = LogManager.logManager.getLogger("")
        rootLogger.setLevel(Level.FINE)
        rootLogger.addHandler(new ConsoleHandler(level:Level.FINE))

        GrettyServer server = [
            webContexts: [
                "/" : [
                    public: {
                        get("/scenario") {
                            response.text = """
@Typed package p

import org.mbte.gretty.httpclient.GrettyWebsocketClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.*
import org.jboss.netty.logging.InternalLoggerFactory
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.*

String address = binding.getProperty("address")

class ClientManager {
    Timer timer = []

    Executor threadPool = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())

    Semaphore connectLimit = [100]

    volatile int attemptsToConnect
    volatile int pendingConnects
    volatile int connected

    ClientManager (String address) {
        def nioExecutor = Executors.newFixedThreadPool(128)
        NioClientSocketChannelFactory factory = [nioExecutor, nioExecutor]

        for(i in 0..<1000) {
            startNewClient (i, address, factory)
        }
        startClockThread ()
        Thread.sleep(24*60*60*1000)
    }

    void startClockThread () {
        def logger = InternalLoggerFactory.getInstance(ClientManager)
        Thread clockThread = [
            daemon: true,
            name: "Pulse Clock Thread",
            run: {
                def stat = 0

                while(true) {
                    Thread.sleep(250)

                    if (++stat == 12) { // 3 sec
                        stat = 0
                        logger.info "Attempts: \$attemptsToConnect Pending: \$pendingConnects Connected: \$connected"
                    }
                }
            }
        ]
        clockThread.start ()
    }

    void startNewClient (int i, String address, ChannelFactory factory) {
        AtomicInteger counter = [0]
        GrettyWebsocketClient client = [
            'super' : [new InetSocketAddress(address, 8080), "/ws"],

            timerTask: (TimerTask)null,

            onWebSocketConnect: {
                pendingConnects.decrementAndGet ()
                connected.incrementAndGet()

                timerTask = {
                    threadPool.execute {
                        send("\$i: Hello, world! \$counter")
                        counter.incrementAndGet()
                    }
                }
                timer.scheduleAtFixedRate (timerTask, 1000, 25000+Math.random()*10000)
            },

            onConnectFailed: {
                pendingConnects.decrementAndGet ()

                timer.schedule({ tryConnect(this) }, 1000)
            },

            onDisconnect: {
                connected.decrementAndGet ()

                timerTask?.cancel ()
                timerTask = null
                timer.schedule({ tryConnect(this) }, 1000)
            }
        ]

        threadPool.execute { tryConnect(client) }
    }

    void tryConnect (GrettyWebsocketClient client) {
        attemptsToConnect.incrementAndGet()
          pendingConnects.incrementAndGet()

        threadPool.execute {
            connectLimit.acquire ()
            client.connect ().addListener { future ->
                connectLimit.release ()
            }
        }
    }
}
new ClientManager(address)
                            """
                        }

                        websocket("/ws") { event ->
                            switch(event) {
                                case String:
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
        server.start()

        def osBean = ManagementFactory.getOperatingSystemMXBean()
        def memBean = ManagementFactory.memoryMXBean

        def logger = InternalLoggerFactory.getInstance(Performance)
        Thread clockThread = [
            daemon: true,
            name: "Pulse Clock Thread",
            run: {
                def last = System.currentTimeMillis()
                while(true) {
                    Thread.sleep(3000)

                    def increase = System.currentTimeMillis() - last
                    last += increase
                    logger.info """${server.allConnected.size()} channels.
 Heap: ${memBean.heapMemoryUsage}
NHeap: ${memBean.nonHeapMemoryUsage}
Sent:  ${server.ioMonitor.bytesSent.getAndSet(0)*1000.0d/increase}b/s   Received: ${server.ioMonitor.bytesReceived.getAndSet(0)*1000.0d/increase}b/s"""
                }
            }
        ]
        clockThread.start ()
    }

    static void startClient(String address) {
        def pool = Executors.newFixedThreadPool(1)
        GrettyClient client = [new InetSocketAddress(address, 8080)]
        client.connect().addListener { future ->
            if (future.success) {
                GrettyHttpRequest req = ["/scenario"]
                req.addHeader HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE
                client.request(req).whenBound { bound ->
                    pool.execute {
                        def script = bound.get().content.toString(CharsetUtil.UTF_8)
                        println script
                        new GroovyShell(new Binding([address:address])).evaluate(script)
                    }
                    client.disconnect ()
                }
            }
            else {
                startClient address
            }
        }
    }
}

//java -Xmx6500m -XX:+UseConcMarkSweepGC -server -cp /usr/local/java/classes/production/Groovypp:/usr/local/java/classes/production/StdLib:/usr/local/java/classes/production/resource-ec2:/usr/local/java/classes/production/Gretty:/usr/local/java/classes/test/Gretty:/usr/local/java/gpp/lib/*:/usr/local/java/apache-resource-0.6.1/lib/*:/usr/local/java/resource-ec2-libs/lib/*:/usr/local/java/gretty-libs/lib/*  org.mbte.gretty.httpserver.Performance ec2-184-72-146-4.compute-1.amazonaws.com
