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
import org.mbte.gretty.httpserver.template.GrettyTemplateEngine
import org.mbte.gretty.httpserver.template.GrettyTemplateScript
import groovypp.text.FastStringWriter

/**
 * Corresponds to sub-path of application.
 */
@Typed class GrettyContext {
    GrettyContext(String path = null) {
        if(path) {
            dir = path
        }
    }

    /**
     * Class loader for templates
     */
    GrettyTemplateEngine templateEngine = [this.class.classLoader]

    /**
     * Server this context belongs too
     */
    protected GrettyServer server

    /**
     * Parent context. All paths are relative to that context
     */
    protected GrettyContext parentContext

    /**
     * Sub-path corresponding to this context
     */
    protected String     webPath

    /**
     * Path where to find static files for this context
     */
    protected String     staticFiles

    /**
     * Path where to find templates for this context
     */
    protected String     templateFiles

    /**
     * Path where to find templates for this context
     */
    protected String     groovletFiles

    /**
     * Path for static resources to be searched in class path
     */
    String               staticResources

    /**
     * Class loader for static resources
     */
    ClassLoader          staticResourcesClassLoader = this.class.classLoader

    /**
     * Default handler for this context. Used if no static file/resource or more specific handler found
     */
    protected GrettyHttpHandler defaultHandler

    protected Map<String,GrettyWebSocketHandler> webSockets = [:]

    private Map<HttpMethod,List<HandlerMatcher>> handlers = [:]

    protected Map<String,GrettyContext> webContexts = [:]

    protected GrettyContext findContext(String uri) {
        if(uri.startsWith(webPath)) {
            webContexts.entrySet().find { wc -> uri.startsWith(wc.key) }?.value ?: this
        }
    }

    void handleHttpRequest(GrettyHttpRequest request, GrettyHttpResponse response) {
        def localUri = URLDecoder.decode(request.path, "UTF-8").substring(webPath?.length())

        // try static file
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

        // try static resource
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

        def groovyFile = findGroovletScript(localUri)
        if (groovyFile) {
            response.status = groovyFile.second
            if (groovyFile.second.code != 200) {
                response.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8")
                response.responseBody = "Failure: ${groovyFile.second}\r\n"
            }
            else {
                def template = templateEngine.getTemplateClass(groovyFile.first)
                GrettyTemplateScript script = template.newInstance() [request: request, response: response]
                def writer = new FastStringWriter()
                script.writeTo([:], writer)
                response.html = writer.toString()
            }
            return
        }


        // try matchers
        def override = request.getHeader('X-HTTP-Method-Override')
        if(override) {
            override = HttpMethod.valueOf(override)
        }
        else {
            override = request.method
        }
        def methodHandlers = handlers [override]

        for(matcher in methodHandlers) {
            def pathArgs = matcher.doesMatch(localUri)
            if(pathArgs != null) {
                matcher.handler.handle(request, response, pathArgs)
                return
            }
        }

        // own default handler (maybe delegates to parent default)
        if (defaultHandler) {
            defaultHandler.handle(request, response, Collections.emptyMap())
            return
        }

        response.status = NOT_FOUND
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
        response.responseBody = "Failure: ${NOT_FOUND}\r\n"
    }

    public void initContext (String path, GrettyContext parentContext, GrettyServer server) {
        this.server = server
        this.parentContext = parentContext

        if(!defaultHandler)
            defaultHandler = parentContext?.defaultHandler

        webPath = path.endsWith("/") ? path.substring(0,path.length()-1) : path

        if(!staticFiles && parentContext?.staticFiles) {
            staticFiles = (parentContext.staticFiles + "/" + webPath).replace('/', File.separatorChar)
        }

        if(!staticResources && parentContext?.staticResources) {
            staticResources = (parentContext.staticResources + "/" + webPath).replace('/', File.separatorChar)
        }

        if(staticFiles) {
            def file = new File(staticFiles).canonicalFile
            if(!file.exists() || !file.directory || file.hidden) {
                throw new IOException("directory $staticFiles does not exists or hidden")
            }
            else {
                staticFiles = file.absoluteFile.canonicalPath
            }
        }

        if (webSockets) {
            for(ws in webSockets.entrySet()) {
                ws.value.socketPath = ws.key
                ws.value.server = server
            }
        }

        for(matcherList in handlers.values())
            for(matcher in matcherList) {
                matcher.handler.server = server
                matcher.handler.context = this
            }

        if(defaultHandler) {
            defaultHandler.server = server
            defaultHandler.context = this
        }

        webContexts = webContexts.sort { me1, me2 -> me2.key <=> me1.key }

        for(e in webContexts.entrySet()) {
            e.value.initContext(e.key, this, server)
        }
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

    private Pair<File, HttpResponseStatus> findGroovletScript(String uri) {
        if (!groovletFiles)
            return null

        uri = uri.replace('/', File.separatorChar)

        if(uri.startsWith('/'))
            uri = uri.substring(1)

        if (uri.contains(File.separator + ".") || uri.contains("." + File.separator) || uri.startsWith(".") || uri.endsWith(".")) {
            return [null,FORBIDDEN]
        }

        def fileUri = "$groovletFiles/${uri}.groovy"

        def file = new File(fileUri).canonicalFile
        if (!file.exists() || !file.file) {
            fileUri = "$groovletFiles/${uri}.gpp"
            file = new File(fileUri).canonicalFile
            if (!file.exists() || !file.file) {
                fileUri = "$groovletFiles/${uri}.gpptl"
                file = new File(fileUri).canonicalFile
                if (!file.exists() || !file.file) {
                    file = new File(file.parentFile, "default.groovy").canonicalFile
                    if (!file.exists() || !file.file) {
                        return null
                    }
                }
            }
        }

        if (file.hidden) {
            return [null,FORBIDDEN]
        }

        if (!file.file) {
            def indexFiles = file.listFiles { dir, name -> name == "default.groovy" }
            if (indexFiles?.length == 1 && new File(file, "default.groovy").file)
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

        def url = staticResourcesClassLoader.getResource("WEB-INF/$staticResources/$uri")
        if(!url) {
            url = staticResourcesClassLoader.getResource("$staticResources/$uri")

            if(!url)
                return null
        }

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

    void setDir (String dir) {
        this.staticFiles = new File(dir, "static").canonicalFile.absolutePath
        this.groovletFiles = this.templateFiles = new File(dir).canonicalFile.absolutePath
    }

    void setPublic (GrettyPublicDescription description) {
        description.context = this
        description.run ()
    }

    GrettyHttpHandler addHandler(HttpMethod httpMethod, String match, GrettyHttpHandler handler) {
        def methodHandlers = handlers [httpMethod]
        if (!methodHandlers) {
            methodHandlers = []
            handlers [httpMethod] = methodHandlers
        }
        methodHandlers.add((HandlerMatcher)[match: match, handler: handler])
        handler
    }

    void addWebSocket(String path, GrettyWebSocketHandler handler) {
        handler.socketPath = path
        webSockets [path] = handler
    }

    void setUnresolvedProperty(String name, GrettyRestDescription description) {
        description[match:name, context: this].run ()
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
                       uri = ''
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

            if(uri.length() > 0)
                return null

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
