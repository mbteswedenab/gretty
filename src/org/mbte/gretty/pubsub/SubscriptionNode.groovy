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
import groovypp.concurrent.FHashMap

@Typed class SubscriptionNode {
    private FList<Subscriber>                 subscribers = FList.emptyList
    private FHashMap<String,SubscriptionNode> children = FHashMap.emptyMap

    protected FList<String> interestedIn = FList.emptyList

    SubscriptionNode subscribe(FList<String> pathFromRoot, Subscriber channel) {
        if(pathFromRoot.empty) {
            [subscribers: subscribers + channel, children: children]
        }
        else {
            def c = children.get(pathFromRoot.head) ?: new SubscriptionNode()
            c = c.subscribe(pathFromRoot.tail, channel)
            [subscribers: subscribers, children: children.put(pathFromRoot.head, c)]
        }
    }

    protected SubscriptionNode unsubscribe(FList<String> pathFromRoot, Subscriber channel) {
        if(pathFromRoot.empty) {
            def c = subscribers - channel
            c === subscribers ? this : [channels: c, children: children]
        }
        else {
            def c = children.get(pathFromRoot.head)
            if(!c)
                this
            else{
                def cc = c.unsubscribe(pathFromRoot.tail, channel)
                if(cc === c) {
                    this
                }
                else {
                    [subscribers: subscribers,
                        children: cc.children.empty && cc.subscribers.empty ?
                            children.remove(pathFromRoot.head)
                          : children.put(pathFromRoot.head, cc)
                    ]
                }
            }
        }
    }

    protected void post(FList<String> pathFromRoot, Object message) {
        if(pathFromRoot.empty) {
            for(c in subscribers)
                c.post message
        }
        else {
            children.get(pathFromRoot.head)?.post pathFromRoot.tail, message
        }
    }

    protected FList<String> getInterestedIn() {
        if(!subscribers.empty || children.size() != 1) {
            FList.emptyList
        }
        else{
            def entry = children.entrySet().iterator().asList()[0]
            entry.value.interestedIn + entry.key
        }
    }
}
