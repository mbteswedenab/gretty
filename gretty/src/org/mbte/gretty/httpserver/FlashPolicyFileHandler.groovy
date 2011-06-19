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

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers

@Typed class FlashPolicyFileHandler extends SimpleChannelHandler {
    private final GrettyServer server

    private static final byte [] policy = "<policy-file-request/>".bytes

    FlashPolicyFileHandler(GrettyServer server) {
        this.server = server
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ChannelBuffer cb = e.message
        if(cb.readableBytes() >= policy.length) {
            def ri = cb.readerIndex()
            def match = true
            for(int i = 0; i != policy.length; ++i) {
                if(cb.getByte(ri + i) != policy[i]) {
                    match = false
                    break
                }
            }

            if(match) {
                def xmlRes = "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\"/></cross-domain-policy> "
                def res = ChannelBuffers.wrappedBuffer(xmlRes.bytes)
                res.setByte(res.capacity()-1, 0)
                e.channel.write(res).addListener {
                    e.channel.close()
                }
                return 
            }
        }
        super.messageReceived(ctx, e)
    }
}
