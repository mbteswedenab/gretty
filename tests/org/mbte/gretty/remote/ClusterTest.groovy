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

package org.mbte.gretty.remote

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import groovypp.concurrent.CallLaterExecutors
import java.util.concurrent.atomic.AtomicInteger

@Typed class ClusterTest extends GroovyTestCase {
    void testStartStop () {
        def n = 3
        def stopCdl = new CountDownLatch(n)
        def connectCdl = new CountDownLatch(n*(n-1))
        def disconnectCdl = new CountDownLatch(n*(n-1))
        def pool = CallLaterExecutors.newCachedThreadPool()
        for(i in 0..<n) {
            def counter = new AtomicInteger(n-1)
            ClusterNode cluster = [executor:pool, multicastPort:4238]
            cluster.communicationEvents.subscribe { msg ->
                println "${cluster.id} $msg"
            }
            cluster.communicationEvents.subscribe { msg ->
                switch (msg) {
                    case ClusterNode.CommunicationEvent.Connected:
                        msg.remoteNode << "Hello!"
                        connectCdl.countDown()
                        break
                    case ClusterNode.CommunicationEvent.Disconnected:
                        disconnectCdl.countDown()
                        break
                }
            }
            cluster.mainActor = { msg ->
                println "${cluster.id} received '$msg' $counter"
                if (!counter.decrementAndGet()) {
                    println "${cluster.id} stopping"
                    cluster.shutdown {
                        stopCdl.countDown()
                        println "${cluster.id} stopped"
                    }
                }
            }

            cluster.startup()
        }
        assertTrue(connectCdl.await(100,TimeUnit.SECONDS))
        println "all connections succeeded"
        assertTrue(stopCdl.await(100,TimeUnit.SECONDS))
        println "all stops succeeded"
        assertTrue(disconnectCdl.await(100,TimeUnit.SECONDS))
        println "all disconnects succeeded"
    }
}
