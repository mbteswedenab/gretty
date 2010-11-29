@Typed package gretty

import org.mbte.gretty.httpserver.GrettyServer
import java.util.logging.Level
import java.util.logging.ConsoleHandler
import java.util.logging.LogManager
import org.jboss.netty.logging.InternalLogLevel
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.codehaus.jackson.map.ObjectMapper
import org.mbte.gretty.httpserver.GrettyContext
import org.mbte.gretty.cassandra.AsyncCassandra
import org.mbte.gretty.cassandra.AsyncColumnPath
import org.mbte.gretty.cassandra.AsyncKeyspace
import org.mbte.gretty.cassandra.thrift.ConsistencyLevel

def rootLogger = LogManager.logManager.getLogger("")
rootLogger.setLevel(Level.FINE)
rootLogger.addHandler(new ConsoleHandler(level:Level.FINE))

GrettyServer server = [
    logLevel: InternalLogLevel.DEBUG,

    static: "./rootFiles",

    default: { response.html = template("./templates/404.ftl", [user:"Dear Unknown User"]) },

    public: {
        get("googlib/:path") {
            redirect "http://ajax.googleapis.com/ajax/libs/" + it.path
        }
    },

    webContexts: [
        "/websockets" : [
            static: "./webSocketsFiles",

            default: {
                response.redirect("http://${request.getHeader('Host')}/websockets/")
            },

            public: {
                get("/:none") { args ->
                    if(!args.none.empty)
                        response.redirect("http://${request.getHeader('Host')}/websockets/")
                    else
                        response.responseBody = new File("./webSocketsFiles/ws.html")
                }

                websocket("/ws",[
                    onMessage: { msg ->
                        socket.send(msg.toUpperCase())
                    },

                    onConnect: {
                        socket.send("Welcome!")
                    }
                ])
            },
        ],

        "/life" : LifeGame,
    ]
]
server.start()

class User {
    static final Map<String,User> users = [:]

    String name, password, id

    Map<String,Game> games = [:]

    Game newGame (int width, int height) {
        Game game = [id: UUID.randomUUID(), width:width, height:height]
        games[game.id] = game
    }
}

class Game {
    String id

    int width, height

    List<Pair<Integer,Integer>> liveCells = []
}

class LifeGame extends GrettyContext  {
    AsyncCassandra  cassandra = [["localhost"]]
    AsyncKeyspace   keyspace  = [cassandra, "Keyspace1"]
    AsyncColumnPath userName  = [keyspace, "Standard1", "userName"]

    static class AuthRequest {
        String userId
        String password
    }

    static class Coord {
        int x, y
        String toString() {"{$x,$y}"}
    }

    static class GamePosition {
        int width
        int height

        List<Coord> live

        String toString() {"{$width,$height,$live}"}

        GamePosition next () {
            println this
            GamePosition res = [width:width, height:height, live:[]]

            def cells = new boolean [width][height]

            for(c in live) {
                cells[c.x][c.y] = true
            }

            for(i in 0..<width) {
                for(j in 0..<height) {
                    def around = 0
                    around += i>0&&j>0&&cells [i-1][j-1] ? 1 : 0
                    around += i>0&&cells [i-1][j  ] ? 1 : 0
                    around += i>0&&j<47&&cells [i-1][j+1] ? 1 : 0
                    around += j>0&&cells [i  ][j-1] ? 1 : 0
                    around += j<47&&cells [i  ][j+1] ? 1 : 0
                    around += i<47&&j>0&&cells [i+1][j-1] ? 1 : 0
                    around += i<47&&cells [i+1][j  ] ? 1 : 0
                    around += i<47&&j<47&&cells [i+1][j+1] ? 1 : 0

                    def nlive = cells[i][j]
                    if(around<2||around>3)
                        nlive = false
                    if(around==3)
                        nlive = true

                    if(nlive)
                        res.live << [x:i, y:j]
                }
            }

            res
        }
    }

    {
        setStatic("./lifeFiles")
        
        setDefault{
            response.redirect("http://${request.getHeader('Host')}/life")
        }

        setPublic{
            get(":game") {
                switch(it.game) {
                    case "":
                        response.responseBody = new File("./lifeFiles/life.html")
                    break

                    default:
                        
                    break
                }
            }

            post("/auth:register"){
                def authRequest = AuthRequest.fromJson(request.contentText)
                switch(it.register) {
                    case "/register":
                        // new user registration
                        response.async = true
                        userName.get(authRequest.userId, ConsistencyLevel.ONE) {
                            try {
                                String value = it.get().column.value.fromSerialBytes()
                            }
                            catch(Throwable t) {
                                response.complete()
                                return
                            }
                        }
                    break

                    case "":
                        // autentification
                    break

                    default:
                        return // 403 response
                }
            }

            websocket("", [
                onMessage: { msg ->
                    println msg
                    def pos = GamePosition.fromJson(msg)
                    def res = pos.next().toJson()
                    println res
                    socket.send(res)
                }
            ])
        }
    }
}