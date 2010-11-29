package org.mbte.gretty.httpserver

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.WriteCompletionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.buffer.ChannelBuffer

@Typed class IoMonitor extends SimpleChannelHandler {

    volatile long bytesSent, bytesReceived
    volatile long totalBytesSent, totalBytesReceived

    void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) {
        bytesSent.addAndGet(e.writtenAmount)
        totalBytesSent.addAndGet(e.writtenAmount)
        super.writeComplete(ctx, e)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        bytesReceived.addAndGet(((ChannelBuffer)e.message).readableBytes())
        totalBytesReceived.addAndGet(((ChannelBuffer)e.message).readableBytes())
        super.messageReceived(ctx, e)
    }

    long getAndCleanSent () {
        bytesSent.getAndSet(0)
    }

    long getAndCleanReceived () {
        bytesReceived.getAndSet(0)
    }
}
