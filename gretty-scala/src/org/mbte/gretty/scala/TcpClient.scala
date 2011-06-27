/*
 * Gretty Framework
 *     Copyright (C) 2008-2011  MBTE Sweden AB
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mbte.gretty.scala

import java.net.SocketAddress
import org.jboss.netty.channel._

class TcpClient {
//  val delegate = Gretty.getInstance.createTcpClient
//
//  /**
//   * Connect the client to the server
//   * @return
//   */
//  def connect: ChannelFuture = { delegate.connect() }
//
//  /**
//   * Connect the client to the server
//   */
//  def connect( fn: (ChannelFuture) => Any) {
//    delegate.connect(new ChannelFutureListener {
//      def operationComplete(future: ChannelFuture) {
//        fn(future)
//      }
//    })
//  }
//
//  /**
//   * @return is the client connected
//   */
//  def isConnected: Boolean = { delegate.isConnected }
//
//  def remoteAddress(remoteAddress: SocketAddress) : TcpClient = {delegate.remoteAddress(remoteAddress); this }
//
//  def handler(handler: IChannelHandler) : TcpClient = { delegate.handler(handler); this }
//
//  def localAddress(localAddress: SocketAddress) : TcpClient = { delegate.localAddress(localAddress); this }
//
//  def onBuildPipeline(fn: (ChannelPipeline) => Any) = {
//    delegate.onBuildPipeline(new BuildPipelineEventHandler {
//      def onBuildPipeline(pipeline: ChannelPipeline) {
//        fn(pipeline)
//      }
//    })
//    this
//  }
//
//  def textProtocol(fn: (MessageEvent) => Boolean) = {
//    delegate.onBuildPipeline(new BuildPipelineEventHandler {
//      def onBuildPipeline(pipeline: ChannelPipeline) {
//        BuildPipelineEventHandler.addTextProtocol(pipeline)
//      }
//    })
//    delegate.onMessage(new MessageEventHandler {
//      def onMessage(event: MessageEvent) = fn(event);
//    })
//    this
//  }
//
//  def onMessage(fn: (MessageEvent) => Boolean) : TcpClient = {
//    delegate.onMessage(new MessageEventHandler {
//      def onMessage(event: MessageEvent): Boolean = {
//        fn(event)
//      }
//    })
//    this
//  }
//
//  def onException(fn: (Channel,Throwable) => Any) : TcpClient = {
//    delegate.onException(new ExceptionEventHandler {
//      def onException(channel: Channel, throwable: Throwable) {
//        fn(channel, throwable)
//      }
//    })
//    this
//  }
//
//  def onClose(fn: (Channel) => Any) : TcpClient = {
//    delegate.onClose(new ChannelEventHandler {
//      def onChannelEvent(channel: Channel) {
//        fn(channel)
//      }
//    })
//    this
//  }
//
//  def onDisconnect(fn: (Channel) => Any) : TcpClient = {
//    delegate.onDisconnect(new ChannelEventHandler {
//      def onChannelEvent(channel: Channel) {
//        fn(channel)
//      }
//    })
//    this
//  }
//
//  def onConnect(fn: (Channel) => Any) : TcpClient = {
//    delegate.onConnect(new ChannelEventHandler {
//      def onChannelEvent(channel: Channel) {
//        fn(channel)
//      }
//    })
//    this
//  }
}