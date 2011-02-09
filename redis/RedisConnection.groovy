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

import redis.clients.jedis.Jedis
import redis.clients.jedis.Client
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import groovypp.channels.ExecutingChannel
import groovypp.channels.SupervisedChannel

/**
 * @author Alex Tkachman
 */
@Typed class RedisConnection extends SupervisedChannel<RedisMessageBroker> {
    static final String CLUSTER_PING = "cluster:ping"
    static final String MESSAGES = "cluster:messages:"

    final String clusterId = UUID.randomUUID()

    private volatile boolean stopped

    protected void doStartup() {
//        executor.execute {
//            executor.execute {
//                Client client = [owner.host, owner.port]
//                client.subscribe CLUSTER_PING, "$MESSAGES$id"
//                client.getObjectMultiBulkReply()
//                client.getObjectMultiBulkReply()
//
//                while (!stopped) {
//                    try {
//                        def reply = client.getObjectMultiBulkReply()
//                        owner << new RedisMessageBroker.Message(data:reply)
//                    }
//                    catch (e) {
//                        if (!(e.cause instanceof SocketTimeoutException)) {
//                            stop()
//                            break
//                        }
//                    }
//                }
//
//                client.disconnect()
//            }
//
//        }
    }

    void stop() {
//        stopped = true
//        if (cdl.getCount() > 0)
//            cdl.countDown()
    }
}
