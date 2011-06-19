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

package org.mbte.gretty.remote.inet

import groovypp.channels.LoopChannel
import groovypp.channels.SupervisedChannel

@Typed abstract class MulticastChannel extends LoopChannel {
    InetAddress multicastGroup
    int         multicastPort

    private MulticastSocket socket

    void doStartup() {
        socket = new MulticastSocket(multicastPort)
        socket.joinGroup(multicastGroup)
        super.doStartup()
    }

    static class Sender extends MulticastChannel {
        byte [] dataToTransmit

        boolean doLoopAction () {
            if (!stopped) {
                socket.send ([dataToTransmit, dataToTransmit.length, multicastGroup, multicastPort])
            } else {
                socket.close()
            }
            return !stopped
        }
    }

    static class Receiver extends MulticastChannel {
        boolean doLoopAction () {
          if (!stopped) {
              def buffer = new byte[512]
              def packet = new DatagramPacket(buffer, buffer.length)
              socket.receive(packet)
              ((SupervisedChannel)owner).post(InetDiscoveryInfo.fromBytes(buffer))
          } else {
              socket.close()
          }
          return !stopped
        }
    }
}