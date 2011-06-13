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

import org.mbte.chat.model.User
import com.mongodb.MongoException

@Typed class UserTest extends ChatAppTest {
    void testInit () {
        assert chatApp.userDao === chatApp.loginService.userDao
    }

    void testUserCreation () {
        def user = new User(id:UUID.randomUUID(), userName: 'Test User 1', password:'Test User 1 password')
        chatApp.userDao.save(user)

        def user2 = chatApp.userDao.get(user.id)

        assertEquals user.userName, user2.userName
        assertEquals user.password, user2.password

        def user3 = chatApp.userDao.findByName(user.userName)

        assertEquals user.userName, user3.userName
        assertEquals user.password, user3.password
    }

    void testDuplicateUser () {
        def thrown = false
        try {
            chatApp.userDao.save(new User(id:UUID.randomUUID(), userName: 'Test User 1', password:'Test User 1 password'))
            chatApp.userDao.save(new User(id:UUID.randomUUID(), userName: 'Test User 1', password:'Test User 1 password'))
        }
        catch(MongoException.DuplicateKey e) {
            thrown = true
        }
        assert thrown
    }

    void testLogin () {
        def session = chatApp.loginService.login("User1","password")
        assert !session

        def user = chatApp.userDao.newUser()[userName: 'User1', password:'password']
        chatApp.userDao.save(user)

        session = chatApp.loginService.login("User1","password")
        assert session

        def logonUser = chatApp.loginService.login(session)
        assert logonUser.id == user.id
    }
}
