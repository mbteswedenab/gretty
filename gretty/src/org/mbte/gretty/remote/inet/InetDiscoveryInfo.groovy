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

import groovypp.channels.MessageChannel

@Typed class InetDiscoveryInfo {
    UUID clusterId
    InetSocketAddress serverAddress

    static final long MAGIC = 0x23982392L;

    static byte [] toBytes(UUID clusterId, InetSocketAddress serverAddress) {
        def out = new ByteArrayOutputStream()
        def stream = new DataOutputStream(out)

        stream.writeLong(MAGIC)
        stream.writeLong(clusterId.getMostSignificantBits())
        stream.writeLong(clusterId.getLeastSignificantBits())
        stream.writeInt(serverAddress.port)
        def addrBytes = serverAddress.address.address
        stream.writeInt(addrBytes.length)
        stream.write(addrBytes)
        stream.close()
        out.toByteArray()
    }

    static InetDiscoveryInfo fromBytes (byte [] buf) {
        def input = new DataInputStream(new ByteArrayInputStream(buf))
        if (input.readLong() == MAGIC) {
            def uuid = new UUID(input.readLong(), input.readLong())
            def port = input.readInt()
            def addrLen = input.readInt()
            def addrBuf = new byte [addrLen]
            input.read(addrBuf)
            [clusterId:uuid, serverAddress:new InetSocketAddress(InetAddress.getByAddress(addrBuf), port)]
        }
    }

    void post(MessageChannel channel) {
        channel?.post(this) 
    }
}
