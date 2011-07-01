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





package org.mbte.gretty.httpserver

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*
import org.jboss.netty.handler.codec.http.*
import org.jboss.netty.handler.codec.base64.Base64
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.buffer.ChannelBuffer
import org.mbte.gretty.JacksonCategory

@Typed
@Use(JacksonCategory)
class GrettyHttpRequest extends DefaultHttpRequest {

    private String path
    private Map<String, String> params
    private boolean followRedirects = false

    Set<Cookie> cookies

    String charset = "UTF-8"

    GrettyHttpRequest() {
        super(HttpVersion.HTTP_1_1, HttpMethod.GET, "")
    }

    GrettyHttpRequest(HttpVersion httpVersion = HttpVersion.HTTP_1_1, HttpMethod method = HttpMethod.GET, String uri) {
        super(httpVersion, method, uri)
    }

    void setUri(String uri) {
        super.setUri(uri)

        path = null
        params = null
    }

    String getPath () {
        if(path == null) {
            def pathEndPos = uri.indexOf('?')
            path = pathEndPos < 0 ? uri : uri.substring(0, pathEndPos)
        }
        path
    }

    Map<String, String> getParameters() {
        if(params == null) {
            def decoded = new QueryStringDecoder(uri).parameters
            params = new LinkedHashMap()
            for (e in decoded.entrySet()) {
                params[e.key] = e.value.get(0)
            }
        }
        params
    }
    
    void setParameters(Map<String,String> params) {
//        def encoder = new QueryStringEncoder(decoder.path)
//        for(param in params) {
//            encoder.addParam param.key, v
//        }
    }

    void setContent(ChannelBuffer obj) {
        if (content?.readableBytes() > 0)
            throw new IllegalStateException("Body of http request already set")

        if (!getHeader(CONTENT_LENGTH))
            setHeader(CONTENT_LENGTH, obj.readableBytes())

        super.setContent(obj)
    }

    String getContentText () {
        new String(content.array(), content.arrayOffset(), content.readableBytes())
    }

    void setText(Object body) {
        setHeader(CONTENT_TYPE, "text/plain; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    void setHtml(Object body) {
        setHeader(CONTENT_TYPE, "text/html; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    void setJson(Object body) {
        setHeader(CONTENT_TYPE, "application/json; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body instanceof String ? body.toString() : body.toJsonString(), charset)
    }

    void setXml(Object body) {
        setHeader(CONTENT_TYPE, "application/xml; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    GrettyHttpRequest setAuthorization (String user, String password) {
        def line = "$user:$password"
        def cb = ChannelBuffers.wrappedBuffer(line.bytes)
        cb = Base64.encode(cb)
        line = new String(cb.array(), cb.arrayOffset(), cb.readableBytes())
        setHeader(HttpHeaders.Names.AUTHORIZATION, "Basic $line")
        this
    }

    GrettyHttpRequest keepAlive () {
        setHeader HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE
        this
    }

    GrettyHttpRequest followRedirects (boolean follow) {
        followRedirects = follow
        this
    }

    boolean followRedirects () {
        followRedirects
    }

    void setMethodOverride(HttpMethod newMethod) {
        method = HttpMethod.POST
        addHeader("X-HTTP-Method-Override", newMethod)
    }

    Cookie getCookie(String name) {
        if(cookies) {
            for (c in cookies)
                if(c.name == name)
                    return c
        }
    }

    void addCookie(Cookie cookie) {
        if(!cookies)
            cookies = []
        cookies << cookie
    }

    void addCookie(String name, String value) {
        addCookie(new DefaultCookie(name, value))
    }
}
