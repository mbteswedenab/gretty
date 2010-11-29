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

package org.mbte.gretty.httpserver

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.ConcurrentHashMap

@Typed class PseudoWebSocketManager {
    final ExecutorService clientPool = Executors.newFixedThreadPool(30)

    private final ConcurrentHashMap<String, PseudoWebSocket> map = []

    final Timer timer = ["CometManager@${hashCode()}", true]

    PseudoWebSocket allocateId (GrettyWebSocketHandler handler) {
        PseudoWebSocket newClient = [this]
        while(true) {
            def uuid = UUID.randomUUID().toString()
            newClient.sessionId = uuid
            if(map.putIfAbsent(uuid, newClient) == null) {
                newClient.handler = handler.clone()
                return newClient
            }
        }
    }

    PseudoWebSocket getClient(String id) {
        map.get(id)
    }
}
