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

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.logging.InternalLogLevel
import org.jboss.netty.logging.InternalLogger
import org.jboss.netty.logging.InternalLoggerFactory

import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ExceptionEvent

@Typed class HttpLoggingHandler extends SimpleChannelHandler {

    private final InternalLogger logger
    private final InternalLogLevel level

    HttpLoggingHandler (InternalLogLevel level = InternalLogLevel.DEBUG) {
        logger = InternalLoggerFactory.getInstance(getClass())
        this.level = level
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        log(e)
        super.messageReceived(ctx, e)
    }

    void writeRequested(ChannelHandlerContext ctx, MessageEvent e) {
        log(e)
        super.writeRequested(ctx, e)
    }

    def void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        log(e)
        super.exceptionCaught(ctx, e)
    }

    public void log(ChannelEvent e) {
        if (logger.isEnabled(level)) {
            String msg = e.toString();

            // Log the message (and exception if available.)
            if (e instanceof ExceptionEvent) {
                logger.log(level, msg, ((ExceptionEvent) e).getCause());
            } else {
                logger.log(level, msg);
            }
        }
    }
}
