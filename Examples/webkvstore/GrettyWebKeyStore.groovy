@Typed package org.mbte.gretty.examples

import org.mbte.gretty.httpserver.GrettyServer
import java.util.concurrent.ConcurrentHashMap

def server = new GrettyServer()

ConcurrentHashMap map = [:]

server [
    localAddress: new InetSocketAddress(9000),

    // where to find static resources
    static: "./static",

    default: {
        response.redirect "/webkvstore.html"
    },

    public: {
        // redirect googlib path to google servers
        get("/googlib/:path") {
            redirect "http://ajax.googleapis.com/ajax/libs/${it.path}"
        }

        get("/api/:key") {
            if(it.key)
                response.json = [result: map.get(it.key)]
            else
                response.json = [result: map.keySet()]
        }

        post("/api/:key") {
            response.json = [result: map.put(it.key, request.contentText)]
        }

        delete("/api/:key") {
            response.json = [result: map.remove(it.key)]
        }
    }
]

server.start()