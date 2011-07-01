/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */





package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpclient.HttpRequestHelper
import sun.security.util.Debug

@Typed class ContextTest extends GroovyShellTestCase implements HttpRequestHelper {
    GrettyServer server

    protected void setUp() {
        super.setUp()

        server = [
            LocalAddress: new LocalAddress("test_server"),

            "/main" : {
                get {
                    response.html = "main"
                }
            },

            webContexts: [
                "/subcontext": [
                    default: {
                        response.html = "default"
                    },

                    "/api2/:command": {
                        get {
                            response.html = "api2: ${request.parameters.command}"
                        }
                    },

                    public: {
                        get("/api/:command") {
                            response.html = "api: ${request.parameters.command}"
                        }
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop()
        super.tearDown()
    }

    void testMe () {
        doTest("/subcontext/api/239") { response ->
            assert response.contentText == 'api: 239'
        }

        doTest("/subcontext/api2/239") { response ->
            assert response.contentText == 'api2: 239'
        }

        doTest("/subcontext/api239") { response ->
            assert response.contentText == 'default'
        }

        doTest("/main") { response ->
            assert response.contentText == 'main'
        }
    }
}
