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

import com.google.inject.Inject
import org.mbte.chat.model.User
import org.mbte.chat.model.UserDAO
import com.hazelcast.core.HazelcastInstance
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Typed class LoginService {
    @Inject UserDAO userDao
    @Inject HazelcastInstance hazelcast

    private Executor cacheExecutor = Executors.newFixedThreadPool(10)

    String login(String userName, String password) {
        def user = userDao.findByName(userName)

        if(user?.password == password) {
            def session = UUID.randomUUID().toString()
            hazelcast.getMap("sessions").put(session, user.id, 20, TimeUnit.MINUTES)
            session
        }
    }

    User login(String session) {
        String userId = hazelcast.getMap("sessions").get(session)
        if(userId) {
            userDao.get(userId)
        }
    }
}
