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

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*

import static org.jboss.netty.handler.codec.http.HttpVersion.*

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.codehaus.groovy.reflection.ReflectionCache
import org.objectweb.asm.Opcodes
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.stream.ChunkedStream
import org.jboss.netty.handler.codec.http.DefaultHttpChunk
import org.jboss.netty.handler.codec.http.DefaultHttpChunkTrailer

@Typed class GrettyHttpResponse extends DefaultHttpResponse {

    Object responseBody

    String charset = "UTF-8"

    boolean async

    private volatile Channel channel
    private boolean keepAlive

    GrettyHttpResponse (Channel channel, boolean keepAlive) {
        super(HTTP_1_1, HttpResponseStatus.FORBIDDEN)
        this.channel = channel
        this.keepAlive = keepAlive
    }

    GrettyHttpResponse (HttpVersion version, HttpResponseStatus status) {
        super(version, status)
    }

    Integer getChannelId() {
        channel?.id
    }

    void write(Object obj) {
        channel.write(obj)
    }

    void complete() {
        def channel = this.channel

        async = true
        if (!channel)
            return

        this.channel = null

        def writeFuture = channel.write(this)
        if (responseBody) {
            writeFuture = channel.write(responseBody)
        }

        if (!keepAlive || status.code >= 400) {
            writeFuture.addListener(ChannelFutureListener.CLOSE)
        }
    }

    void setContent(ChannelBuffer obj) {
        if (responseBody || content.readableBytes() > 0)
            throw new IllegalStateException("Body of http response already set")

        if (status == HttpResponseStatus.FORBIDDEN) {
            status = HttpResponseStatus.OK
        }

        if (!getHeader(CONTENT_LENGTH))
            setHeader(CONTENT_LENGTH, obj.readableBytes())
        super.setContent(obj)
    }

    void setResponseBody(Object obj) {
        if (responseBody || content.readableBytes() > 0)
            throw new IllegalStateException("Body of http response already set")

        if (status == HttpResponseStatus.FORBIDDEN) {
            status = HttpResponseStatus.OK
        }

        switch(obj) {
            case String:
                content = ChannelBuffers.copiedBuffer(obj, charset)
            break

            case File:
                setHeader(HttpHeaders.Names.CONTENT_LENGTH, obj.length())
                this.responseBody = obj
            break

            case InputStream:
                def bytes = obj.bytes
                setHeader(HttpHeaders.Names.CONTENT_LENGTH, bytes.length)
                this.responseBody = ChannelBuffers.wrappedBuffer(bytes)
            break

            default:
                this.responseBody = obj
        }
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
        content = ChannelBuffers.copiedBuffer(body instanceof String ? body.toString() : body.toJson(), charset)
    }

    void setXml(Object body) {
        setHeader(CONTENT_TYPE, "application/xml; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    void redirect(String where) {
        status = HttpResponseStatus.MOVED_PERMANENTLY
        setHeader HttpHeaders.Names.LOCATION, where
        setHeader HttpHeaders.Names.CONTENT_LENGTH, 0
    }

    static String toJson (Object obj) {
        if(!obj)
            return 'null'

        switch(obj) {
            case Map:
                StringBuilder sb = []
                sb << '{'
                def first = true
                for(e in obj.entrySet()) {
                    if(!first) {
                        sb << ','
                    }
                    first = false
                    sb << "\"${e.key}\":${e.value.toJson()}"
                }
                sb << '}'
                return sb

            case List:
                StringBuilder sb = []
                sb << '['
                def first = true
                for(e in obj) {
                    if(!first) {
                        sb << ','
                    }
                    first = false
                    sb << e.toJson()
                }
                sb << ']'
                return sb

            case String:
                return "\"${obj}\""

            case Number:
                return obj

            default:
                def clazz = ReflectionCache.getCachedClass(obj.class)
                StringBuilder sb = []
                sb << '{'
                def list = clazz.fields.toList()
                def first = true
                for(f in list) {
                    if((f.modifiers & (Opcodes.ACC_STATIC|Opcodes.ACC_TRANSIENT)))
                        continue
                    if(!first) {
                        sb << ','
                    }
                    first = false
                    sb << "\"${f.name}\":${f.getProperty(obj).toJson()}"
                }
                sb << '}'
                return sb
        }
    }

    String getContentText () {
        new String(content.array(), content.arrayOffset(), content.readableBytes())
    }

    void close() {
        channel?.close()
    }
}
