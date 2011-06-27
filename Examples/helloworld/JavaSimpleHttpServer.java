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

import org.mbte.gretty.httpserver.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class JavaSimpleHttpServer {
    public static void main(String[] args) {
        GrettyServer server = new GrettyServer()
            .localAddress(new InetSocketAddress("localhost", 8080))
            .dir("web")
            .defaultHandler(new GrettyHttpHandler() {
                public void handle(Map<String, String> pathArguments) {
                    getResponse().redirect("/");
                }
            })
            .handler("/", new GrettyRestDescription() {
                public void run() {
                    get(new GrettyHttpHandler() {
                        public void handle(Map<String, String> pathArguments) {
                            Map binding = new HashMap();
                            binding.put("title", "Hello, World!");
                            binding.put("message", "Hello, Java World");
                            getResponse().setHtml(template("web/templates/main.gpptl", binding));
                        }
                    });
                }
            })
            .webContext("/myapp", new GrettyContext("myapp"));
        server.start();

        GrettyHttpResponse response;

        response = server.doTest("/nosuchurl");
        assert response.getStatus().getCode() == 301;
        assert response.getHeader("Location").equals("/");

        response = server.doTest("/");
        assert response.getStatus().getCode() == 200;
        String respBody = response.getContentText();
        assert respBody.contains("<title>Hello, World!</title>");
        assert respBody.contains("Hello, Dynamic World!(from template)<br>");
        assert respBody.contains("<center>HEADER</center>");
        assert respBody.contains("<center>FOOTER</center>");

        server.stop();
    }
}
