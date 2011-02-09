/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.gretty.redis

import groovypp.channels.SupervisedChannel

@Typed class RedisMessageBroker extends SupervisedChannel {
    String host
    int port

    final Timer timer = []

    final String id = UUID.randomUUID()

    private final Set<String> known = []

    protected void doStartup() {
        startupChild new RedisClusterPoller()
    }

    protected void onClusterStatus(Collection<String> list) {
        schedule {
            println "$id : $list"
        }
    }
}
