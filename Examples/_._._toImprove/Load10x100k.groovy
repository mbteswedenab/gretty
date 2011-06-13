@Typed package examples

import java.nio.ByteBuffer
import groovypp.channels.MultiplexorChannel
import org.mbte.gretty.httpserver.GrettyServer
import org.jboss.netty.logging.InternalLogLevel

class Data {
    static Random random = []

    static Thread update = [
        run: {
            while(true) {
                def start = System.currentTimeMillis()
                def bytes = ByteBuffer.allocate(32)
                for(i in 0..<8)
                    bytes.putInt(random.nextInt())
                multi << bytes
                def end = System.currentTimeMillis() - start
                if(end < 40) {
                    Thread.sleep(40-end)
                }
            }
        }
    ]

    static MultiplexorChannel multi = []
}

Data.update.start ()

GrettyServer server = [
    logLevel: InternalLogLevel.DEBUG,

    webContexts: [
        "/" : [
            public: {
                websocket("/ws",[
                    onMessage: { msg ->
                    },

                    onConnect: {
                        Data.multi.subscribe { msg ->
                            socket.send msg
                        }
                    }
                ])
            },
        ],
    ]
]
server.start()
