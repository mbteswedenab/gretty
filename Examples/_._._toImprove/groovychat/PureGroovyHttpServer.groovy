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

import org.mbte.gretty.httpserver.GrettyServer
import org.mbte.gretty.JacksonCategory
import org.mbte.gretty.httpserver.GrettyHttpRequest
import org.mbte.gretty.httpserver.GrettyHttpResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import org.mbte.gretty.httpserver.GrettyHttpHandler

def facebook = new FacebookService()

def server = new GrettyServer()
server.groovy = [

    localAddress: new InetSocketAddress(8081),

    staticResources: "META-INF/socket.io/",

    static: "./static",

    default: {
        response.redirect "/chat"
    },

    public: {
        get("/chat") {
            def accessToken = facebook.accessToken(request, response)

            if(!accessToken) {
                response.html = template("./templates/fb.ftl", [applicationId: FacebookService.applicationId])
                return
            }

            facebook.withUser(accessToken,thisHandler) { user ->
                response.html = template("./templates/fb.ftl", [
                    applicationId: FacebookService.applicationId,
                    user: user,
                    accessToken: accessToken]
                )
            }
        }

        iosocket("/chat", new ChatHistory()) {
            onConnect {
            }

            onDisconnect {
                if(socketPrivate)
                    broadcast([announcement: "${socketPrivate} left".toString()])
            }

            onMessage { msg ->
                if(msg.post) {
                    socketShared.push msg
                    broadcast msg
                }
                else if(msg.history) {
                    if(!socketPrivate) {
                        socketPrivate = msg.userName
                        broadcast([announcement: "${socketPrivate} joined".toString()])
                    }
                    send (buffer: socketShared.recent)
                }
            }
        }
    }
]
server.start ()

class ChatHistory {
    private LinkedList list = []

    void push(Object o) {
        synchronized(list) {
            list.addLast o
            if(list.size() >= 20)
                list.removeFirst()
        }
    }

    List getRecent() {
        synchronized(list) {
            list.clone()
        }
    }
}

class FacebookService {
    static def applicationId = "211731558851278"

    private ConcurrentHashMap<String, Map> users = new ConcurrentHashMap<String, Map>()

    private Executor executor = Executors.newFixedThreadPool(10)

    static String accessToken(GrettyHttpRequest request, GrettyHttpResponse response) {
        String accessToken
        def cookieName = "fbs_$applicationId"
        def fbCookie = request.getCookie(cookieName)
        if(fbCookie) {
            for(t in fbCookie[0].value.tokenize('&')) {
                if(t.startsWith("access_token")) {
                    accessToken = t.substring(1 + 'access_token'.length())
                }
            }
        }

        if(!accessToken) {
            response.cookies(["$cookieName":''])
        }

        accessToken
    }

    void withUser (String accessToken, GrettyHttpHandler handler, Closure operation) {
        def user = users[accessToken]
        if(user)
            operation(user)
        else
            handler.async(executor) {
                use(JacksonCategory) {
                    try {
                        def text = "https://graph.facebook.com/me?access_token=$accessToken".toURL().text
                        users[accessToken] = user = Map.fromJson(text)
                    }
                    catch(e) {
                        users[accessToken] = null
                    }

                    operation(user)
                }
            }
    }
}
