package org.mbte.gretty.httpserver

import org.mbte.gretty.JacksonCategory

@Typed
@Use(JacksonCategory)
class JsonTest extends GroovyTestCase {
    static class Game {
        String id

        int width
        int height
        List<Pair<Integer,Integer>> liveCells = []
    }
    
    void testMap () {
        Game game = [id: "0", width:12, height:13, liveCells:[[10,22], [11,4], [22,4]]]
        assertEquals '{"id":"0","width":12,"height":13,"liveCells":[{"first":10,"second":22},{"first":11,"second":4},{"first":22,"second":4}]}',  game.toJsonString()
    }
}

