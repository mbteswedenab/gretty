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

package org.mbte.gretty

import _root_.scala.Predef
import junit.framework.TestCase
import org.jboss.netty.handler.codec.string.{StringEncoder, StringDecoder}
import java.util.concurrent.CountDownLatch
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel._
import scala.{TcpClient, TcpServer}

class GrettyScalaTest extends TestCase {

//  val server = new TcpServer().
//        localAddress(new LocalAddress("test_server")).
//        onMessage{ event =>
//            val message: String = event.getMessage.asInstanceOf[String]
//            System.out.println(message)
//            event.getChannel.write(message)
//            false
//        }.
//        onConnect{ channel =>
//          System.out.println("connected " + channel.toString)
//        }.
//        onDisconnect{ channel =>
//          System.out.println("disconnected " + channel.toString)
//        }.
//        onBuildPipeline { pipeline =>
//            pipeline.addBefore("this", "string.decoder", new StringDecoder)
//            pipeline.addBefore("this", "string.encoder", new StringEncoder)
//        }
//
//  override def setUp() = server.start()
//
//  override def tearDown() = server.stop()
//
//  def testMe () {
//    val countDownLatch = new CountDownLatch(1)
//
//    val client = new TcpClient().
//          remoteAddress(new LocalAddress("test_server")).
//          textProtocol{ event =>
//            val message: String = event.getMessage.asInstanceOf[String]
//            Predef.assert("Hello, World!" == message)
//            countDownLatch.countDown
//            false
//          }.
//          onConnect{ channel =>
//            System.out.println("client connected " + channel.toString)
//          }.
//          onDisconnect{ channel =>
//            System.out.println("client disconnected " + channel.toString)
//          }
//
//    client.connect.addListener(new ChannelFutureListener {
//      def operationComplete(future: ChannelFuture): Unit = {
//        future.getChannel.write("Hello, World!\n")
//      }
//    })
//
//    countDownLatch.await
//  }
}