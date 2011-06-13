@Typed package org.mbte.gretty.examples

import org.mbte.gretty.httpserver.GrettyServer
import java.util.concurrent.ConcurrentHashMap
import org.mbte.gretty.httpserver.GrettyContext
import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.HttpRequestHelper
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion
import org.mbte.gretty.httpserver.GrettyHttpRequest
import junit.textui.TestRunner
import org.mbte.gretty.JacksonCategory
import org.w3c.dom.Text

ConcurrentHashMap map = [:]

String [] args = binding.variables.args
def test = args?.length && args[0] == 'test'

new GrettyServer() [
    localAddress: !test ? new InetSocketAddress(9000) : new LocalAddress("test_server"),

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

        get("/api/:operation/:key") {
            switch(it.operation) {
                case "get":
                    if(it.key)
                        response.json = [result: map.get(it.key)]
                    else
                        response.json = [result: map.keySet()]
                break

                case "delete":
                    response.json = [result: map.remove(it.key)]
                break
            }
        }

        post("/api/put/:key") {
            response.json = [result: map.put(it.key, request.contentText)]
        }
    },

    webContexts: [
        // alternative approach. use of web context
       "/api2" : [
           public: {
               get("/:operation/:key") {
                   switch(it.operation) {
                       case "get":
                           if(it.key)
                               response.json = [result: map.get(it.key)]
                           else
                               response.json = [result: map.keySet()]
                       break

                       case "delete":
                           response.json = [result: map.remove(it.key)]
                       break
                   }
               }

               post("/put/:key") {
                   response.json = [result: map.put(it.key, request.contentText)]
               }
           }
       ],

        // another alternative - dedicated class for GrettyContext
        "/api3" : new Api3 (map: map)
    ]
].start()

class Api3 extends GrettyContext {
    ConcurrentHashMap map

    Api3 () {
        this [
            public: {
                get("/:operation/:key") {
                    switch(it.operation) {
                        case "get":
                            if(it.key)
                                response.json = [result: map.get(it.key)]
                            else
                                response.json = [result: map.keySet()]
                        break

                        case "delete":
                            response.json = [result: map.remove(it.key)]
                        break
                    }
                }

                post("/put/:key") {
                    response.json = [result: map.put(it.key, request.contentText)]
                }
           }
        ]
    }
}

if(test) {
    try {
        TestRunner.run(ServerTest)
    }
    finally {
        System.exit(0)
    }
}

@Use(JacksonCategory)
class ServerTest extends GroovyTestCase implements HttpRequestHelper {
    void testMe () {
        GrettyHttpRequest req

        req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/put/1"]
        req.text = "2"
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == null
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/get/1"]
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == '2'
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/put/1"]
        req.text = "3"
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == '2'
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/api2/get/1"]
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == '3'
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/api2/put/1"]
        req.text = "2"
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == '3'
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/api3/get/1"]
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == '2'
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/api3/delete/1"]
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == '2'
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/api2/get/1"]
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == null
        }

        req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/api3/put/1"]
        req.text = "4"
        doTest(req) { response ->
            assert Map.fromJson(response.contentText).result == null
        }
    }
}