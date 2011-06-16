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

package org.mbte.gretty.httpserver.session

import groovypp.concurrent.FHashMap
import org.mbte.gretty.httpserver.GrettyServer

@Typed class GrettySession implements Externalizable {
    private volatile FHashMap attributes = [:]

    transient GrettyServer server

    GrettySession () {
        maxInactiveInterval = 20*60 // 20 minutes
        lastAccessedTime = System.currentTimeMillis()
    }

    /**
     * Session id
     */
    String id

    /**
     * Time in seconds after which the session will be expired
     */
    int    maxInactiveInterval

    /**
     * Milliseconds since midnight Jan 1st 1970
     */
    long   lastAccessedTime

    def getUnresolvedProperty(String name) {
        attributes.get(name)
    }

    void setUnresolvedProperty(String name, def value) {
        attributes = attributes.put(name, value)
    }
}
