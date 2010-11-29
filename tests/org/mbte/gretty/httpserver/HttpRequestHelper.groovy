package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.GrettyClient
import groovypp.concurrent.BindLater
import org.jboss.netty.handler.codec.http.HttpResponse

@Trait abstract class HttpRequestHelper {

    void doTest (String request, Function1<HttpResponse,Void> action) {
        BindLater cdl = []

        GrettyClient client = [new LocalAddress("test_server")]
        try {
            client.connect{ future ->
                client.request(new GrettyHttpRequest(request)) { bound ->
                    try {
                        action(bound.get())
                        cdl.set("")
                    }
                    catch(e) {
                        cdl.setException(e)
                    }
                }
            }

            cdl.get()
        }
        finally {
            client.disconnect ()
        }
    }

    void doTest (GrettyHttpRequest request, Function1<GrettyHttpResponse,Void> action) {
        BindLater cdl = []

        GrettyClient client = [new LocalAddress("test_server")]
        client.connect{ future ->
            client.request(request) { bound ->
                try {
                    action(bound.get())
                    cdl.set("")
                }
                catch(e) {
                    cdl.setException(e)
                }
            }
        }

        cdl.get()
        client.disconnect ()
    }
}
