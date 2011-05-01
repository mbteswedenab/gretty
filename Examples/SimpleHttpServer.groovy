@Typed package examples

import org.mbte.gretty.httpserver.GrettyServer

GrettyServer server = [
    staticResources: "META-INF/web-socket-js",

    localAddress: new InetSocketAddress("localhost", 8080),

    public: {
        websocket("/") { msg ->
            println msg
        }
    }
]
server.start ()
