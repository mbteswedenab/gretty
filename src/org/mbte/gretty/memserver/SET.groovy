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
package org.mbte.gretty.memserver

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.DynamicChannelBuffer
import java.nio.ByteBuffer

@Typed class SET extends Command<Void> {
    String  key
    byte [] value

    void execute(Map<String,byte[]> map) {
        map.put(key, value)
        def buffer = new DynamicChannelBuffer(4)
        buffer.writeInt(CMD_SET)
        channel.write(buffer)
    }

    static SET read(ChannelBuffer buffer) {
        def start = buffer.readerIndex()

        if (buffer.readableBytes() < 4) {
            buffer.readerIndex(start - 4)
            return null;
        }

        def keyLen = buffer.readInt();

        if (buffer.readableBytes() < keyLen + 4) {
            buffer.readerIndex(start - 4)
            return null;
        }

        def ri = buffer.readerIndex();
        buffer.skipBytes(keyLen);

        def valueLen = buffer.readInt();

        if (buffer.readableBytes() < valueLen) {
            buffer.readerIndex(start - 4)
            return null;
        }

        buffer.skipBytes(valueLen)

        def keyBytes = new byte[keyLen]
        buffer.getBytes(ri, keyBytes)

        def valueBytes = new byte[valueLen]
        buffer.getBytes(ri + 4 + keyLen, valueBytes)

        return new SET(key: new String(keyBytes, "UTF-8"), value: valueBytes)
    }

    Object decodeResponse(ChannelBuffer buffer) {
        if(buffer.readableBytes() < 4) {
            return null;
        }
        buffer.skipBytes(4)
        return Command.RESP_OK;
    }

    ChannelBuffer encode() {
        def keyLen = key.utf8Length()
        def buffer = ChannelBuffers.buffer(4+4+keyLen+4+value.length)
        buffer.writeInt(Command.CMD_SET)
        buffer.writeUtf8(key, keyLen)
        buffer.writeInt(value.length)
        buffer.writeBytes(value)
        buffer
    }

    int getCode() {
        return CMD_SET
    }
}
