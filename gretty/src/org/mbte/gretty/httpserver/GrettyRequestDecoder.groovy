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

import org.jboss.netty.handler.codec.http.HttpVersion

import org.jboss.netty.handler.codec.http.HttpMessage

import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.HttpMessageDecoder

@Typed public class GrettyRequestDecoder extends HttpRequestDecoder {

    protected HttpMessage createMessage(String[] initialLine) throws Exception{
        return new GrettyHttpRequest(
                HttpVersion.valueOf(initialLine[2]), HttpMethod.valueOf(initialLine[0]), initialLine[1]);
    }

    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, HttpMessageDecoder.State state) {
        def decode = super.decode(ctx, channel, buffer, state)
        if(decode instanceof GrettyHttpRequest) {
            def header = decode.getHeader("Cookie")
            if(header) {
                def decoder = new CookieDecoder()
                decode.cookies = decoder.decode(header)
            }
        }
        return decode
    }
}
