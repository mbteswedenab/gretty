/*
 * Copyright 2009-2011 MBTE Sweden AB.
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

package org.mbte.gretty.hazelcast

import org.jboss.netty.handler.codec.http.HttpMethod
import org.mbte.gretty.test.GrettyServerTestCase
import org.mbte.gretty.httpserver.GrettyHttpRequest
import org.mbte.gretty.httpserver.GrettyHttpResponse
import com.hazelcast.config.MapConfig

@Typed class SessionsTest extends GrettyServerTestCase {
    protected void buildServer() {
        GrettyHazelcastManager hazelcast = []
        MapConfig mc = []
        mc.setName(GrettyHazelcastSessionManager.GRETTY_SESSION_MAP)
        hazelcast.config.addMapConfig(mc)
        server = [
            sessionManager: new GrettyHazelcastSessionManager(hazelcast),

            defaultHandler: {
                Integer obj = session.counter
                if(obj == null) {
                    session.counter = obj = 1
                }
                else {
                    obj = obj + 1
                    session.counter = obj
                }
                response.text = obj
            },

            "/async": {
                get {
                    getSessionAsync { session ->
                        Integer obj = session.counter
                        if(obj == null) {
                            session.counter = obj = 1
                        }
                        else {
                            obj = obj + 1
                            session.counter = obj
                        }
                        response.text = obj
                    }
                }
            }
        ]
    }

    void testMe () {
        Reference sessionId = []
        for (i in 0..<10) {
            GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/"]
            if(sessionId.get())
                req.addCookie("JSESSIONID", sessionId.get().toString())

            doTest(req) { GrettyHttpResponse response ->
                def session = response.getCookie("JSESSIONID")?.value
                sessionId.set(session)
                println "$i ${response.contentText} $session"
                assert response.contentText == "${i+1}"
            }
        }
    }

    void testAsync () {
        Reference sessionId = []
        for (i in 0..<10) {
            GrettyHttpRequest req = [method:HttpMethod.GET, uri:"/async"]

            if(sessionId.get())
                req.addCookie("JSESSIONID", sessionId.get().toString())

            doTest(req) { GrettyHttpResponse response ->
                def session = response.getCookie("JSESSIONID")?.value
                sessionId.set(session)
                println "$i ${response.contentText} $session"
                assert response.contentText == "${i+1}"
            }
        }
    }
}
