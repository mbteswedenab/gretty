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

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.mongodb.Mongo
import com.google.code.morphia.Morphia
import org.mbte.chat.model.UserDAO
import org.mbte.chat.model.MongoDatastore
import org.mbte.chat.model.ConversationDAO

@Typed class MongoModule extends AbstractModule{
    protected void configure() {
        bind(Mongo).in(Singleton)
        bind(Morphia).in(Singleton)
        bind(MongoDatastore).in(Singleton)
        bind(UserDAO).in(Singleton)
        bind(ConversationDAO).in(Singleton)
    }
}
