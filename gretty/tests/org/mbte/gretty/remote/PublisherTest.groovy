/*
 * Copyright 2009-2011 MBTE Sweden AB.
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

import org.mbte.gretty.pubsub.Publisher
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap

@Typed class PublisherTest extends GroovyTestCase {
    void testMe () {
        ConcurrentHashMap<String, List<String>> res = [software: [],hardware: []]
        Publisher pub = [executor: Executors.newFixedThreadPool(4)]
        pub.subscribe("/jobs/software") { msg ->
            res.software << msg
        }.subscribe("/jobs/hardware") { msg ->
            res.hardware << msg
        }

        pub.publish("/jobs/software",  "apple")
        pub.publish("/jobs/hardware", "cisco")
        pub.publish("/jobs/software",  "microsoft")
        pub.publish("/jobs/software",  "google")
        pub.publish("/jobs/hardware", "ibm")

        Thread.sleep 1000

        assert res == [software:['apple', 'microsoft', 'google'], hardware:['cisco', 'ibm']]
    }
}
