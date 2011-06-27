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
import org.jboss.netty.channel.{ChannelPipeline, MessageEvent, Channel}
import org.mbte.gretty.httpserver.GrettyServer
import org.mbte.gretty.AbstractServer

class TcpServer extends AbstractServer {

//  val delegate = Gretty.getInstance.createTcpServer
//
//  def handler(handler: IChannelHandler) = { delegate.handler(handler); this }
//
//  def localAddress(localAddress: SocketAddress) = { delegate.localAddress(localAddress); this }
//
//  def onBuildPipeline(fn: (ChannelPipeline) => Any)  : TcpServer = {
//    delegate.onBuildPipeline(new BuildPipelineEventHandler {
//      def onBuildPipeline(pipeline: ChannelPipeline) {
//        fn(pipeline)
//      }
//    })
//    this
//  }
//
//  def onMessage(fn: (MessageEvent) => Boolean)  : TcpServer = {
//    delegate.onMessage(new MessageEventHandler {
//      def onMessage(event: MessageEvent): Boolean = {
//        fn(event)
//      }
//    })
//    this
//  }
//
//  def onException(fn: (Channel,Throwable) => Any)  : TcpServer = {
//    delegate.onException(new ExceptionEventHandler {
//      def onException(channel: Channel, throwable: Throwable) {
//        fn(channel, throwable)
//      }
//    })
//    this
//  }
//
//  def onClose(fn: (Channel) => Any)  : TcpServer = {
//    delegate.onClose(new ChannelEventHandler {
//      def onChannelEvent(channel: Channel) {
//        fn(channel)
//      }
//    })
//    this
//  }
//
//  def onDisconnect(fn: (Channel) => Any)  : TcpServer = {
//    delegate.onDisconnect(new ChannelEventHandler {
//      def onChannelEvent(channel: Channel) {
//        fn(channel)
//      }
//    })
//    this
//  }
//
//  def onConnect(fn: (Channel) => Any) : TcpServer = {
//    delegate.onConnect(new ChannelEventHandler {
//      def onChannelEvent(channel: Channel) {
//        fn(channel)
//      }
//    })
//    this
//  }
//
}
