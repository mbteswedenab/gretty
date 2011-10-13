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

import org.mbte.gretty.httpclient.HttpRequestHelper
import org.jboss.netty.channel.local.LocalAddress

@Typed class AGloverTest extends GroovyTestCase implements HttpRequestHelper {
    GrettyServer server = []

    @Dynamic protected void setUp () {
        server.groovy = [
            localAddress: new LocalAddress("test_server"),

            "/app/:name": {
                get {
                    response.text = "Hello, ${request.parameters['name']}"
                }
            } ,
        	"/image/:id":{
        		get {
        			response.text = "Image: $request.parameters.id"
        		}
        	}
        ]
        server.start()
    }

    protected void tearDown () {
        server.stop()
    }

    void testOne () {
        doTest("/app/lambada") { response ->
            assertEquals( "Hello, lambada", response.contentText)
        }
    }

    void testTwo () {
        doTest("/image/200") { response ->
            assertEquals( "Image: 200", response.contentText)
        }
    }
}
