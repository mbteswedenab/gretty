@Typed package examples

import org.mbte.gretty.httpserver.GrettyServer
import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion.Static

GrettyServer server = [
    localAddress: new InetSocketAddress("localhost", 8080),

    default: {
        response.redirect "/"
    },

    static: "static",

    "/": {
        get {
            response.html = template("templates/main.gpptl", [:]) { binding ->
                binding.title = 'Hello, World!'
                binding.message = 'Hello, Static World!'
            }
        }
    }
]
server.start ()
