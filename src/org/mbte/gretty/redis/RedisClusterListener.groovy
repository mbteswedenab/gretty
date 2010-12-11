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
import redis.clients.jedis.Jedis

@Typed class RedisClusterListener extends SupervisedChannel<RedisMessageBroker> {
    static final String CLUSTER_ID_COUNTER = "cluster:id-counter"
    static final String CLUSTER_ID = "cluster:id:"
    static final String CLUSTER_ID_PATTERN = "cluster:id:*"

    private Jedis jedis

    private final Random random = []

    private String ownerIdKey

    void doStartup() {
        jedis = [owner.host, owner.port]
        ownerIdKey = "$CLUSTER_ID${owner.id}"
        poll ()
        super.doStartup()
    }

    @Override
    void doShutdown() {
        jedis?.disconnect()
        super.doShutdown()
    }

    private void poll() {
        if(!stopped()) {
            def resultList = jedis.pipelined {
                client.setex ownerIdKey, 15, owner.id
                client.keys CLUSTER_ID_PATTERN
            }

            owner.onClusterStatus( ((List<String>)resultList[1]).map{ it.substring(11) } )

            schedule (1000*(3 + random.nextInt(4))) {
                 poll ()
            }
        }
    }
}
