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

import org.mbte.gretty.httpserver.session.GrettySessionManager
import org.mbte.gretty.httpserver.session.GrettySession
import groovypp.concurrent.BindLater
import com.hazelcast.core.IMap
import java.util.concurrent.TimeUnit

@Typed class GrettyHazelcastSessionManager extends GrettySessionManager {
    final static String GRETTY_SESSION_MAP = "gretty:sessions"

    private GrettyHazelcastManager hazelcast

    private IMap<String, GrettySession> map

    GrettyHazelcastSessionManager(GrettyHazelcastManager hazelcast) {
        this.hazelcast = hazelcast

        def mapConfig = hazelcast.config.getMapConfig(GRETTY_SESSION_MAP)
        if(!mapConfig) {
            throw new IllegalStateException("""Map '$GRETTY_SESSION_MAP' is not configured in Hazelcast
Use org.mbte.gretty.hazelcast.GrettyHazelcastManager to configure.
""")
        }
    }

    GrettySession getSession(String id) {
        map.get(id)[server: server]
    }

    BindLater<GrettySession> getSessionAsync(String id, GrettySessionManager.SessionCallback callback = null) {
        def later = callback ?: new BindLater()
        server.execute {
            later.set(map.get(id)[server: server])
        }
        later
    }

    void removeSession(GrettySession session) {
        server.execute {
            map.remove(session.id)
        }
    }

    void storeSession(GrettySession session) {
        server.execute {
            map.put(session.id, session, session.getMaxInactiveInterval(), TimeUnit.SECONDS)
        }
    }

    void start () {
        hazelcast.start()
        map = hazelcast.instance.getMap(GRETTY_SESSION_MAP)
    }

    void stop () {
        map.flush()
        map= null
        hazelcast.stop()
    }
}
