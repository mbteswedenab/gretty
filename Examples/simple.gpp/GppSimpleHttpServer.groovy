@Typed package examples

import org.mbte.gretty.httpserver.GrettyServer

GrettyServer server = [
    localAddress: new InetSocketAddress("localhost", 8080),

    default: {
        response.redirect "/"
    },

    "/": {
        get {
            response.html = template("./templates/main.gpptl") { binding ->
                binding.title = 'Hello, World!'
                binding.message = 'Hello, Static World!'
            }
        }
    }
]
server.start ()
