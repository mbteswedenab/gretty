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

package org.mbte.gretty.pubsub

import groovypp.concurrent.FList
import java.util.concurrent.Executor

@Typed class Publisher {
    protected volatile SubscriptionNode root = []
    Executor executor

    final Publisher subscribe(FList<String> pathFromRoot = FList.emptyList, Subscriber channel) {
        if(!channel)
            throw new NullPointerException("null subscriber")
        if(channel.publisher)
            throw new IllegalStateException("Subscribed already to another publisher")
        channel.publisher = this

        for(;;) {
            def r = root
            def rr = r.subscribe(pathFromRoot, channel)
            if(r === rr || root.compareAndSet(r, rr)) {
                updateInterests(rr.interestedIn)
                return this
            }
        }
    }

    final Publisher subscribe(String pathFromRoot, Subscriber channel) {
        subscribe(getPublishPath(pathFromRoot), channel)
    }

    final Publisher unsubscribe(FList<String> pathFromRoot = FList.emptyList, Subscriber channel) {
        if(!channel)
            throw new NullPointerException("null subscriber")
        if(channel.publisher !== this)
            throw new IllegalStateException("Not subscribed to this publisher")

        channel.publisher = null

        for(;;) {
            def r = root
            def rr = r.unsubscribe(pathFromRoot, channel)
            if(r === rr || root.compareAndSet(r, rr)){
                updateInterests(rr.interestedIn)
                return this
            }
        }
    }

    final Publisher unsubscribe(String pathFromRoot, Subscriber channel) {
        unsubscribe(getPublishPath(pathFromRoot), channel)
    }

    void publish(FList<String> pathFromRoot = FList.emptyList, Object message) {
        root.post pathFromRoot, message
    }

    void publish(String pathFromRoot, Object message) {
        publish(getPublishPath(pathFromRoot), message)
    }

    protected void updateInterests(FList<String> interests) {}

    private static FList<String> getPublishPath(String path) {
        def res = FList.emptyList
        while(path.length() > 0) {
            def index = path.lastIndexOf('/')
            if(index == -1) {
                return res + path
            }

            if(index == path.length()-1) {
                path = path.substring(0, path.length()-1)
            }

            res = res + path.substring(index+1)
            path = path.substring(0, index)
        }
        res
    }
}
