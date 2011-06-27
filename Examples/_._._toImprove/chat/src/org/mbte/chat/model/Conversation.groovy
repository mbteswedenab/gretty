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

package org.mbte.chat.model

import com.google.code.morphia.annotations.Id
import com.google.code.morphia.annotations.Embedded

@Typed
class Conversation {
    // any positive number means delayed until
    static final long UC_STATUS_INVITED   = 0
    static final long UC_STATUS_AVAILABLE = 0

    @Id String id

    String subject

    @Embedded List<Message> messages = []
    @Embedded Set<UserInConversation> users = []

    boolean equals(o) {
        o instanceof Conversation && o.id == id
    }

    int hashCode() {
        id?.hashCode()
    }

    static class UserInConversation {
        String userId

        // 0  - invited
        // -1 - available now
        // any positive means Date
        long  status

        boolean equals(o) {
            o instanceof UserInConversation && o.userId == userId
        }

        int hashCode() {
            userId?.hashCode()
        }
    }

    static class Message {
        UUID userId
        Date time

        String text
    }
}
