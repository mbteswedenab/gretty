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

import com.google.inject.Inject
import org.mbte.gretty.httpserver.GrettyServer
import com.google.inject.name.Named
import org.mbte.chat.model.UserDAO
import org.mbte.chat.model.MongoDatastore
import org.mbte.chat.model.ConversationDAO

@Typed
@com.google.inject.Singleton
class ChatApplication {
    @Inject UserDAO userDao
    @Inject ConversationDAO conversationDAO
    @Inject LoginService loginService
    @Inject MongoDatastore mongoDatastore

    @Inject @Named("serverLocalAddress") SocketAddress serverLocalAddress

    GrettyServer server = []

    void start () {
        server.localAddress = serverLocalAddress

        server.webContexts = [
            "/user" : [
                public: {
                    post("/create") {
                        try {
                            def cmd = Map.fromJson(request.contentText)
                            def user = userDao.newUser()[userName:cmd.userName, password:cmd.password]
                            userDao.save (user)
                            response.json = [status:'ok', userId:user.id]
                        }
                        catch(Throwable t) {
                            response.json = [status:'ERROR', message:t.message]
                        }
                    }

                    get("/:id") { args ->
                        def user = userDao.get(args.id)
                        response.json = [userName:user.userName, userId:user.id]
                    }
                }
            ]
        ]

        server.start()
    }

    void stop() {
        server.stop ()
    }
}
