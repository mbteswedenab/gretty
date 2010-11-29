package org.mbte.gretty.httpserver

class JsonTest extends GroovyTestCase {
    static class Game {
        String id

        int width
        int height
        List<Pair<Integer,Integer>> liveCells = []
    }
    
    void testMap () {
        Game game = [id: "0", width:12, height:13, liveCells:[[10,22], [11,4], [22,4]]]
        assertEquals '{"id":"0","width":12,"height":13,"liveCells":[[10,22],[11,4],[22,4]]}',  GrettyHttpResponse.toJson(game)
    }
}

