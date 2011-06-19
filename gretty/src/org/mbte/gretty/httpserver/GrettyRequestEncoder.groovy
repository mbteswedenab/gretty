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

import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.CookieEncoder

@Typed class GrettyRequestEncoder extends HttpRequestEncoder {
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object _msg) {
        def msg = _msg
        if(msg instanceof GrettyHttpRequest) {
            if(msg.cookies) {
                def encoder = new CookieEncoder(false)
                for(c in msg.cookies) {
                    encoder.addCookie(c)
                }
                msg.addHeader("Cookie", encoder.encode())
            }
        }
        return super.encode(ctx, channel, msg)
    }
}
