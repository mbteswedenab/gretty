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



package org.mbte.chat

import com.mongodb.MongoException
import org.mbte.chat.model.User
import org.mbte.chat.model.Conversation
import org.mbte.gretty.JacksonCategory

@Typed class ConversationTest extends ChatAppTest {
    void testNewConversation () {
        def user1 = new User(id:UUID.randomUUID(), userName: 'Test User 1', password:'Test User 1 password')
        chatApp.userDao.save(user1)

        def user2 = new User(id:UUID.randomUUID(), userName: 'Test User 2', password:'Test User 2 password')
        chatApp.userDao.save(user2)

        def conversation = chatApp.conversationDAO.newConversation()[subject:'Test conversation']
        conversation.users << new Conversation.UserInConversation(userId: user1.id, status:-1L)
        conversation.users << new Conversation.UserInConversation(userId: user2.id, status:-1L)
        chatApp.conversationDAO.save (conversation)

        def c = chatApp.conversationDAO.get(conversation.id)
        println c.toJsonString()
    }
}
