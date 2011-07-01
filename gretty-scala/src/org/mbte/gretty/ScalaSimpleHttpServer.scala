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

package org.mbte.gretty

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

import java.net.InetSocketAddress
import java.util.HashMap
import java.util.Map
import org.mbte.gretty.httpserver._

object ScalaSimpleHttpServer {
  def main(args: Array[String]): Unit = {
    val server: GrettyServer = new GrettyServer().
      localAddress(new InetSocketAddress("localhost", 8080)).
      dir("web").
      defaultHandler(new GrettyHttpHandler() {
          def handle() : Unit = {
            getResponse.redirect("/")
          }
        }
      ).
      handler("/", new GrettyRestDescription {
        def run: Unit = {
          get(new GrettyHttpHandler {
            def handle(): Unit = {
              val binding = new HashMap[String,String]
              binding.put("title", "Hello, World!")
              binding.put("message", "Hello, Java World")
              getResponse.setHtml(template("web/templates/main.gpptl", binding))
            }
          })
        }
      }).
      webContext("/myapp", new GrettyContext("myapp"))

    server.start

    var response: GrettyHttpResponse = null
    response = server.doTest("/nosuchurl")
    Predef.assert(response.getStatus.getCode == 301)
    Predef.assert(response.getHeader("Location") == "/")

    response = server.doTest("/")
    Predef.assert(response.getStatus.getCode == 200)

    var respBody = response.getContentText
    Predef.assert(respBody.contains("<title>Hello, World!</title>"))
    Predef.assert(respBody.contains("Hello, Dynamic World!(from template)<br>"))
    Predef.assert(respBody.contains("<center>HEADER</center>"))
    Predef.assert(respBody.contains("<center>FOOTER</center>"))

    server.stop
  }
}