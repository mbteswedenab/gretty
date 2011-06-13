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

package org.mbte.chat

import com.google.inject.Guice
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import org.jboss.netty.channel.local.LocalAddress
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.google.inject.Singleton

@Typed abstract class ChatAppTest extends GroovyTestCase {
    protected ChatApplication chatApp

    protected void setUp() {
        super.setUp()
        chatApp = Guice.createInjector(buildModules()).getInstance(ChatApplication)

        chatApp.loginService.hazelcast.getMap("sessions").clear()
        chatApp.mongoDatastore.clean()

        chatApp.start()
    }

    protected void tearDown() {
        chatApp.loginService.hazelcast.getMap("sessions").clear()
        chatApp.mongoDatastore.clean()

        chatApp.stop()
        super.tearDown()
    }

    protected List<AbstractModule> buildModules () {
        [
            new ChatAppModule(),
            {
                bind(SocketAddress).annotatedWith(Names.named("serverLocalAddress")).toInstance(new LocalAddress("test_server"))
                bind(String).annotatedWith(Names.named("mongoDbName")).toInstance("chatAppTest")
            }
        ]
    }
}
