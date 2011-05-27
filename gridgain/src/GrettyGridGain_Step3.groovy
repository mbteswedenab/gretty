/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Typed package example

import org.mbte.gretty.httpserver.GrettyServer
import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.HttpRequestHelper
import junit.textui.TestRunner
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpHeaders
import junit.framework.TestSuite
import org.gridgain.grid.GridFactory
import org.jboss.netty.handler.codec.http.HttpMethod
import org.mbte.gretty.JacksonCategory
import org.mbte.gretty.httpserver.GrettyAsyncFunction
import org.gridgain.grid.lang.GridInClosure
import org.gridgain.grid.GridFuture

/*
On this step we extend our server with alternative api

The API provides exactly the same functionality but implementation is asynchronous.
That means that every call to GridGain cache is not blocked and we do not consume
resources while waiting for distributed operations

In order to do better testing we refactor a bit previous tests and introduce operation to clean the cache

We also learn how to
- deal with web contexts for better modularization
- process requests in non-blocking fashion
- deal with browsers, which is not aware on http methods other than get and post
*/

// start GridGain and obtain cache instance
def cache = GridFactory.start("spring-cache.xml").cache('replicated')

String [] args = binding.variables.args
def inTestMode = args?.length && args[0] == 'test'

new GrettyServer() [
    // server local address
    // if in test mode we use in-memory communication, otherwise IP
    localAddress: !inTestMode ? new InetSocketAddress(9000) : new LocalAddress("test_server"),

    // directory where to find static resources
    static: "./static",

    // default handler for request which don't match any rule
    default: {
        response.redirect "/webkvstore.html"
    },

    public: {
        // redirect googlib path to google servers
        get("/googlib/:path") {
            def googleAjaxPath = 'http://ajax.googleapis.com/ajax/libs'

            switch(it.path) {
                case 'jquery.js':
                    redirect "${googleAjaxPath}/jquery/1.6.1/jquery.min.js"
                break

                case 'prototype.js':
                    redirect "${googleAjaxPath}/prototype/1.7.0.0/prototype.js"
                break

                default:
                    redirect "${googleAjaxPath}/${it.path}"
            }
        }

        get("/api/:key") {
            if(it.key)
                response.json = [result: cache.get(it.key)]
            else
                response.json = [result: cache.keySet()]
        }

        delete("/api/:key") {
            if(it.key)
                response.json = [result: cache.remove(it.key)]
            else
                response.json = [result: cache.removeAll()]
        }

        put("/api/:key") {
            response.json = [result: cache.put(it.key, request.contentText)]
        }
    },

    webContexts: [
       // alternative approach
       // - use of web context instead of default context
       // - use of asynchronous operations
       "/asyncapi" : [
           public: {
               get("/:key") {
                   if(it.key) {
                       cache.getAsync(it.key) << { f ->
                           response.json = [result: f.get()]
                       }
                   }
                   else {
                       cache.getAllAsync() << { f ->
                           response.json = [result: f.get().keySet()]
                       }
                   }
               }

               delete("/:key") {
                   cache.removeAsync(it.key) << { f ->
                       response.json = [result: f.get()]
                   }
               }

               put("/:key") {
                   cache.putAsync(it.key, request.contentText) << { f ->
                       response.json = [result: f.get()]
                   }
               }
           }
       ]
    ]
].start()

// tests
if(inTestMode) {
    try {
        TestRunner.run(
            new TestSuite(
                BasicTest,
                OperationsTest,
                AsyncOperationsTest
            )
        )
    }
    finally {
        System.exit(0)
    }
}

/**
 * Utility class to make GridGain callback Gretty asynchronious
 */
abstract class GrettyGridInClosure<T> extends GridInClosure<T> implements GrettyAsyncFunction<T, Object> {
    final void apply(T t) {
        handlerAction(t)
    }
}

/**
 * Convenient << method for GridFuture
 */
static <V> void leftShift(GridFuture<V> self, GrettyGridInClosure<GridFuture<V>> listener) {
    self.listenAsync listener
}

class BasicTest extends GroovyTestCase implements HttpRequestHelper {
    void testRedirectToMainPage () {
        doTest("/nosuchurl") { response ->
            assert response.status == HttpResponseStatus.MOVED_PERMANENTLY
            assert response.getHeaders(HttpHeaders.Names.LOCATION)[0] == "/webkvstore.html"
        }
    }

    void testMainPage () {
        doTest("/webkvstore.html") { response ->
            assert response.contentText == new File("./static/webkvstore.html").text
        }
    }

    void testRedirectToGoogle () {
        doTest("/googlib/prototype.js") { response ->
            assert response.status == HttpResponseStatus.MOVED_PERMANENTLY
            assert response.getHeaders(HttpHeaders.Names.LOCATION)[0] == "http://ajax.googleapis.com/ajax/libs/prototype/1.7.0.0/prototype.js"

        }
        doTest("/googlib/jquery.js") { response ->
            assert response.status == HttpResponseStatus.MOVED_PERMANENTLY
            assert response.getHeaders(HttpHeaders.Names.LOCATION)[0] == "http://ajax.googleapis.com/ajax/libs/jquery/1.6.1/jquery.min.js"
        }
    }
}

@Use(JacksonCategory)
abstract class AbstractOperationsTest extends GroovyTestCase implements HttpRequestHelper {
    protected void setUp () {
        super.setUp()
        doTest([uri: "/api/", method: HttpMethod.DELETE]) {}
    }

    void doTestOperations (api) {
        doTest([uri: "/$api/1", method: HttpMethod.PUT, text: '239']) { response ->
            assert Map.fromJson(response.contentText).result == null
        }

        doTest([uri: "/$api/1", method: HttpMethod.PUT, text: '932']) { response ->
            assert Map.fromJson(response.contentText).result == '239'
        }

        doTest([uri: "/$api/1", method: HttpMethod.GET]) { response ->
            assert Map.fromJson(response.contentText).result == '932'
        }

        // request done as 'POST' method with additional header
        // X-HTTP-Method-Override: DELETE
        // this emulates situation of some browsers
        // we use methodOveride here only in order to make sure that it handled correctly
        doTest([uri: "/$api/1", methodOverride: HttpMethod.DELETE ]) { response ->
            assert Map.fromJson(response.contentText).result == '932'
        }

        doTest([uri: "/$api/1", method: HttpMethod.GET]) { response ->
            assert Map.fromJson(response.contentText).result == null
        }

        for (i in 0..<10) {
            doTest([uri: "/$api/$i", method: HttpMethod.PUT, text: 'value $i']) { response ->
                assert Map.fromJson(response.contentText).result == null
            }
        }

        doTest([uri: "/$api/", method: HttpMethod.GET]) { response ->
            List result = Map.fromJson(response.contentText).result
            assert result.size() == 10
            assert result.eachWithIndex{ e, i -> i == e }
        }
    }
}

class OperationsTest extends AbstractOperationsTest {
    void testApi () {
        doTestOperations('api')
    }
}

class AsyncOperationsTest extends AbstractOperationsTest {
    void testApi () {
        doTestOperations('asyncapi')
    }
}