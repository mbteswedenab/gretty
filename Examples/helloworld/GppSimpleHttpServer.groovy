@Typed package examples

import org.mbte.gretty.httpserver.GrettyServer
import org.mbte.gretty.httpserver.GrettyContext

GrettyServer server = [
    localAddress: new InetSocketAddress("localhost", 8080),

    default: {
        response.redirect "/"
    },

    dir: "web",

    "/": {
        get {
            response.html = template("web/templates/main.gpptl", [:]) { binding ->
                binding.title = 'Hello, World!'
                binding.message = 'Hello, Static World!'
            }
        }
    },

    webContexts: [
            "/myapp" : new GrettyContext("myapp")
    ]
]
server.start ()
