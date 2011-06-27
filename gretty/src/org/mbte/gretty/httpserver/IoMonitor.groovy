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

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.WriteCompletionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.logging.InternalLoggerFactory
import org.jboss.netty.logging.InternalLogger
import java.lang.management.ManagementFactory
import org.jboss.netty.channel.ChannelStateEvent

@Typed class IoMonitor extends SimpleChannelHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(IoMonitor)

    private long lastTime

    volatile long bytesSent, bytesReceived
    volatile long totalBytesSent, totalBytesReceived
    volatile long connectedChannels, totalChannels

    volatile long totalHttpRequests, httpRequests

    void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        connectedChannels.incrementAndGet ()
        totalChannels.incrementAndGet ()

        super.channelConnected(ctx, e)    //To change body of overridden methods use File | Settings | File Templates.
    }

    void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        connectedChannels.decrementAndGet ()

        super.channelDisconnected(ctx, e)
    }

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

    void setLogStatistics(boolean log) {
        if(log) {
            lastTime = System.currentTimeMillis()
            new Timer().schedule({
                def curTime = System.currentTimeMillis()
                if(curTime != lastTime) {
                    def osBean = ManagementFactory.getOperatingSystemMXBean()
                    def memBean = ManagementFactory.memoryMXBean

                    logger.info """Server Statistic:
    Open connections:    $connectedChannels
    Total connections:   $totalChannels

    Http requests:       ${(httpRequests.getAndSet(0)*1000d)/(curTime-lastTime)} req/sec
    Total http requests: $totalHttpRequests

    Bytes In:            ${bytesReceived.getAndSet(0)/((curTime-lastTime)*1.024d)} Kb/sec
    Bytes Out:           ${bytesSent.getAndSet(0)/((curTime-lastTime)*1.024d)} Kb/sec
    TotalBytesSent:      $totalBytesSent
    TotalBytesReceived:  $totalBytesReceived

    Heap Memory:         ${memBean.heapMemoryUsage}
    Non Heap Memory:     ${memBean.nonHeapMemoryUsage}
    System load:         ${osBean.systemLoadAverage}"""
                    lastTime = curTime
                }
            }, 5000, 5000)
        }
    }
}
