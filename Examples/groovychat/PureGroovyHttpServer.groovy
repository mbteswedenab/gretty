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

    staticResources: "META-INF/web-socket-js",

    static: "./static",

    default: {
        response.redirect "/chat"
    },

    public: {
        get("/chat") {
            def accessToken = FacebookService.accessToken(request, response)

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

        websocket("/") { event ->
            switch(event) {
                case String:
                    def cmd = Map.fromJson(event)
                    switch(cmd.msgType) {
                        case 'newPost':
                            facebook.addEventToRecents cmd
                            broadcast event
                        break

                        case 'getHistory':
                            send ([events: facebook.recentEvents].toJsonString())
                        break
                    }
                break
            }
        }
    }
]
server.start ()

class FacebookService {
    static def applicationId = "211731558851278"

    private ConcurrentHashMap<String, Map> users = new ConcurrentHashMap<String, Map>()

    private Executor executor = Executors.newFixedThreadPool(10)

    static String accessToken(GrettyHttpRequest request, GrettyHttpResponse response) {
        String accessToken
        def cookieName = "fbs_$applicationId"
        def fbCookie = request.cookies."$cookieName"
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
        def res = users[accessToken]
        if(res)
            operation(res)
        else
            handler.async(executor) {
                use(JacksonCategory) {
                    users[accessToken] = res = Map.fromJson("https://graph.facebook.com/me?access_token=$accessToken".toURL().text)
                    println res
                    operation(res)
                }
            }
    }

    private def recents = new LinkedList()

    void addEventToRecents(Object o) {
        synchronized(recents) {
            recents.addLast o
            if(recents.size() >= 100)
                recents.removeFirst()
        }
    }

    List getRecentEvents() {
        synchronized(recents) {
            def res = recents.clone()
            res
        }
    }
}
