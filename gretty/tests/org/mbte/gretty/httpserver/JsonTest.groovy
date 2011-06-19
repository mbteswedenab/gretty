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

