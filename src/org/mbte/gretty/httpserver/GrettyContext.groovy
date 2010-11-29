/*
 * Copyright 2009-2010 MBTE Sweden AB.
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

import org.jboss.netty.handler.codec.http.HttpHeaders

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*
import org.jboss.netty.handler.codec.http.HttpMethod

@Typed class GrettyContext {
    String               staticFiles
    String               staticResources
    ClassLoader          staticResourcesClassLoader = this.class.classLoader
    protected String     webPath

    ClassLoader          classLoader
    String               classLoaderPath

    GrettyServer server

    GrettyHttpHandler defaultHandler

    protected Map<String,GrettyWebSocketHandler> webSockets = [:]

    private Map<HttpMethod,List<HandlerMatcher>> handlers = [:]

    void handleHttpRequest(GrettyHttpRequest request, GrettyHttpResponse response) {
        def localUri = URLDecoder.decode(request.uri, "UTF-8").substring(webPath?.length())

        def staticFile = request.method == HttpMethod.GET ? findStaticFile(localUri) : null
        if (staticFile) {
            response.status = staticFile.second
            if (staticFile.second.code != 200) {
                response.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8")
                response.responseBody = "Failure: ${staticFile.second}\r\n"
            }
            else {
                response.responseBody = staticFile.first
            }
            return
        }

        def staticResource = request.method == HttpMethod.GET ? findStaticResource(localUri) : null
        if(staticResource) {
            response.status = staticResource.second
            if (staticResource.second.code != 200) {
                response.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8")
                response.responseBody = "Failure: ${staticFile.second}\r\n"
            }
            else {
                response.responseBody = staticResource.first
            }
            return
        }

        def methodHandlers = handlers [request.method]

        for(matcher in methodHandlers) {
            def pathArgs = matcher.doesMatch(localUri)
            if(pathArgs != null) {
                matcher.handler.handle(request, response, pathArgs)
                return
            }
        }

        if (defaultHandler) {
            defaultHandler.handle(request, response, Collections.emptyMap())
            return
        }

        response.status = NOT_FOUND
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
        response.responseBody = "Failure: ${NOT_FOUND}\r\n"
    }

    public void initContext (String path, GrettyServer server) {
        this.server = server
        webPath = path.endsWith("/") ? path.substring(0,path.length()-1) : path

        if(staticFiles) {
            def file = new File(staticFiles).canonicalFile
            if(!file.exists() || !file.directory || file.hidden) {
                throw new IOException("directory $staticFiles does not exists or hidden")
            }

            staticFiles = file.absoluteFile.canonicalPath
        }

        if (webSockets) {
            for(ws in webSockets.entrySet()) {
                ws.value.socketPath = ws.key
            }
        }

        if(defaultHandler)
            defaultHandler.server = server
    }

    private Pair<File, HttpResponseStatus> findStaticFile(String uri) {
        if (!staticFiles)
            return null

        uri = uri.replace('/', File.separatorChar)

        if(uri.startsWith('/'))
            uri = uri.substring(1)

        if (uri.contains(File.separator + ".") || uri.contains("." + File.separator) || uri.startsWith(".") || uri.endsWith(".")) {
            return [null,FORBIDDEN]
        }

        uri = "$staticFiles/$uri".replace('/', File.separatorChar)

        File file = [uri]
        if (!file.exists()) {
            return null
        }
        if (file.hidden) {
            return [null,FORBIDDEN]
        }
        if (!file.file) {
            def indexFiles = file.listFiles { dir, name -> name.startsWith("index.") }
            if (indexFiles?.length == 1)
                file = indexFiles[0]
            else
                return defaultHandler ? null : [null,FORBIDDEN]
        }

        [file,OK]
    }

    private Pair<InputStream, HttpResponseStatus> findStaticResource(String uri) {
        if (!staticResources)
            return null

        if(uri.startsWith('/'))
            uri = uri.substring(1)

        def url = staticResourcesClassLoader.getResource("$staticResources/$uri")
        if(!url)
            return null

        try {
            return [url.openStream(),OK]
        }
        catch(e) {
            return [null,INTERNAL_SERVER_ERROR]
        }
    }

    void setDefault (GrettyHttpHandler handler) {
        defaultHandler = handler
    }

    void setStatic (String staticFiles) {
        this.staticFiles = staticFiles
    }

    String getStatic () {
        staticFiles
    }

    void setPublic (GrettyPublicDescription description) {
        description.context = this
        description.run ()
    }

    void addHandler(HttpMethod httpMethod, String match, GrettyHttpHandler handler) {
        def methodHandlers = handlers [httpMethod]
        if (!methodHandlers) {
            methodHandlers = []
            handlers [httpMethod] = methodHandlers
        }
        methodHandlers.add((HandlerMatcher)[match: match, handler: handler])
    }

    void addWebSocket(String path, GrettyWebSocketHandler handler) {
        handler.socketPath = path
        webSockets [path] = handler
    }

    private static final class HandlerMatcher {
        GrettyHttpHandler handler

        private List<String> match = []

        Map<String,String> doesMatch (String uri) {
            Map map = null
            for(int i = 0; i != match.size(); ++i) {
                def m = match[i]
                if(m.charAt(0) == ':') {
                   if(i == match.size()-1) {
                       if(map == null)
                            map = [:]
                       map[m.substring(1)] = uri
                       break
                   }
                   else {
                       if(map == null)
                            map = [:]

                       def next = uri.indexOf(match[++i])
                       if (next < 0)
                            return null
                       map[m.substring(1)] = uri.substring(0, next)
                       uri = uri.substring(next + match[i].length())
                   }
                }
                else {
                   if(uri.startsWith(m))
                      uri = uri.substring(m.length())
                   else
                      return null
                }
            }

            return map == null ? Collections.emptyMap () : map
        }

        void setMatch (String match) {
            def pattern = ~":\\w+"
            def matcher = pattern.matcher(match)
            def lastStart = 0
            while(lastStart < match.length()) {
                def occur = matcher.find ()
                if(!occur) {
                    this.match.add(match.substring(lastStart))
                    break
                }
                else {
                    def start = matcher.start()
                    if(start != lastStart) {
                        this.match.add(match.substring(lastStart, start))
                        this.match.add(match.substring(start, matcher.end()))
                    }
                    else {
                        this.match.add(match.substring(start))
                        break
                    }
                    lastStart = matcher.end ()
                }
            }
        }
    }
}
