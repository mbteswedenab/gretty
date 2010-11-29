package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress

import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod

@Typed class RestTest extends GroovyTestCase implements HttpRequestHelper {

    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        rest("/data/:mapId/:objectId") {
                            get {
                                response.addHeader("mapId", it.mapId)
                                response.addHeader("objectId",  it.objectId)
                                response.addHeader("method",  "get")
                            }

                            post {
                                response.addHeader("mapId", it.mapId)
                                response.addHeader("objectId",  it.objectId)
                                response.addHeader("method",  "post")
                            }
                        }
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testGet() {
        GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/data/abracadabra/245"]
        doTest(req) { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
            assertEquals "get", response.getHeader("method")
        }
    }

    void testPost() {
        GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/data/abracadabra/245"]
        doTest(req) { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
            assertEquals "post", response.getHeader("method")
        }
    }
}
