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



package org.mbte.gretty.httpserver.session

import groovypp.concurrent.BindLater
import org.mbte.gretty.httpserver.GrettyAsyncFunction

@Typed abstract class GrettySessionManager {
    abstract GrettySession getSession(String id)

    abstract BindLater<GrettySession> getSessionAsync(String id, SessionCallback callback = null)

    abstract GrettySession removeSession(GrettySession session)

    abstract GrettySession storeSession(GrettySession session)

    abstract static class SessionCallback extends BindLater<GrettySession> implements GrettyAsyncFunction<GrettySession,Object> {
        protected void done() {
            super.done()
            handlerAction(get())
        }
    }
}
