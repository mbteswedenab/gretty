package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress

@Typed class ServerTest extends GroovyTestCase implements HttpRequestHelper {

    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        get("/data/:mapId/set/:objectId") {
                            response.addHeader("mapId", it.mapId)
                            response.addHeader("objectId",  it.objectId)
                        }

                        get("/data/") {}
                    },

                    default: {
                        response.addHeader "Default", "true"
                        for(p in request.parameters.entrySet())
                            response.addHeader(p.key, p.value.toString())
                        response.text = "default: path: ${request.path}"
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testDefault () {
        doTest("/data?msg=12&value=33") { response ->
            def bytes = new byte [response.content.readableBytes()]
            response.content.getBytes(0, bytes)
            def text = new String(bytes, "UTF-8")

            assertEquals "default: path: /data", text
            assertEquals "true", response.getHeader("Default")
            assertEquals "[12]", response.getHeader("msg")
            assertEquals "[33]", response.getHeader("value")
        }
    }

    void testMatch() {
        doTest("/data/abracadabra/set/245") { response ->
            assertEquals "abracadabra", response.getHeader("mapId")
            assertEquals "245", response.getHeader("objectId")
        }
    }

    void testNoMatch() {
        doTest("/data") { response ->
            assertNull response.getHeader("mapId")
            assertNull response.getHeader("objectId")
        }
    }
}
