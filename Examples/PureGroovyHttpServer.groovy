package examples

import org.mbte.gretty.httpserver.GrettyServer
import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.GrettyWebsocketClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

// Server part - Note that it does not use Groovy++ but pure Groovy
// Very simple websocket server, which prints incoming messages
// and replies to every message with the same text in uppercase

def server = new GrettyServer()
server.groovy = [
    localAddress: new LocalAddress("test_server"),

    public: {
        websocket("/") { msg ->
            println "Server: Received: '$msg'"
            socket.send msg.toUpperCase ()
        }
    }
]
server.start ()

// Client part
// 100 clients send 200 messages each

@Typed void runClient () {
    def numClient = 100
    def terminateCdl = new CountDownLatch(numClient)

    for(clientNum in 0..<numClient) {
        def id = clientNum
        Thread.start {
            CountDownLatch clientTerminateCdl = [1]

            AtomicInteger sent = []
            def opCount = 200
            GrettyWebsocketClient client = [
                    'super': [new LocalAddress("test_server"), "/"],

                    onWebSocketConnect: {
                        println("Client: #$id connected")
                        send ([client:id, msg: sent.getAndIncrement()].toJsonString())
                    },

                    onMessage: { text ->
                        println "Client: #$id Received: '$text'"
                        def msgNumber = sent.getAndIncrement()
                        if(msgNumber < opCount)
                            send ([client:id, msg: msgNumber].toJsonString())
                        else
                            clientTerminateCdl.countDown()
                    }
            ]
            client.connect ()

            clientTerminateCdl.await()

            client.disconnect ()

            terminateCdl.countDown()
        }
    }

    terminateCdl.await()
    println "DONE"
}

runClient ()

server.stop()
