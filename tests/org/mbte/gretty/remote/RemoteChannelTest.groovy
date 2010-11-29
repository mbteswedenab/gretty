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

import groovypp.concurrent.CallLaterExecutors
import java.util.concurrent.CountDownLatch
import groovypp.channels.RemoteMessageChannel
import groovypp.channels.Channels

@Typed class RemoteChannelTest extends GroovyShellTestCase {
    void testChannel () {
        CountDownLatch cdl = [2*100000]

        def id0 = UUID.randomUUID()
        def id1 = UUID.randomUUID()

        List<RemoteMessageChannel.IOContext> ctx = []

        def pool0 = CallLaterExecutors.newFixedThreadPool()
        def pool1 = CallLaterExecutors.newFixedThreadPool()

        ctx << [
            executor:pool0,
            foreignHost: id1,
            myHost: id0,
            mainActor: Channels.channel { RemoteMessageChannel replyTo ->
                replyTo << "ok"
                cdl.countDown()
            },
            sendBytes: { bytes ->
                ctx[1].schedule {
                    ctx[1].receiveBytes(bytes)
                }
            }
        ]  << [
            executor:pool1,
            foreignHost: id0,
            myHost: id1,
            mainActor: Channels.channel { message ->
                cdl.countDown()
            },
            sendBytes: { bytes ->
                ctx[0].schedule {
                    ctx[0].receiveBytes(bytes)
                }
            }
        ]

        for(i in 0..100000)
            ctx[1].post(new RemoteMessageChannel.ForwardMessageToBeSent(forwardTo:RemoteMessageChannel.IOContext.MAIN_ACTOR_ID, message:ctx[1].mainActor))

        cdl.await()
        pool0.shutdown()
        pool1.shutdown()
    }
}
