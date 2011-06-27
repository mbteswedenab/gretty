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



package org.mbte.gretty.pubsub

import groovypp.channels.QueuedChannel
import groovypp.concurrent.FQueue

@Typed abstract class Subscriber<M> extends QueuedChannel<M> implements Runnable {
    protected Publisher publisher

    protected void signalPost(FQueue<M> oldQueue, FQueue<M> newQueue) {
        if(oldQueue.empty)
            publisher.executor.execute this
    }

    void run() {
        for (;;) {
            def q = queue
            def removed = q.removeFirst()
            if (q.size() == 1) {
                if (queue.compareAndSet(q, busyEmptyQueue)) {
                    onMessage removed.first
                    if (!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                        publisher.executor.execute this
                    }
                    return
                }
            }
            else {
                if (queue.compareAndSet(q, removed.second)) {
                    onMessage removed.first
                    publisher.executor.execute this
                    return
                }
            }
        }
    }
}
