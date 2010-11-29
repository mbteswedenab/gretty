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

import org.jboss.netty.buffer.DynamicChannelBuffer
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.ByteBuffer

@Typed class GET extends Command<ByteBuffer> {
    String key

    ChannelBuffer encode() {
        def keyLen = key.utf8Length()
        def buffer = ChannelBuffers.buffer(4+4+keyLen)
        buffer.writeInt(CMD_GET)
        buffer.writeUtf8(key, keyLen)
        buffer
    }

    static GET read(ChannelBuffer buffer) {
        def start = buffer.readerIndex ()

        if(buffer.readableBytes() < 4) {
            buffer.readerIndex(start-4)
            return null;
        }

        def keyLen = buffer.readInt();

        if(buffer.readableBytes() < keyLen) {
            buffer.readerIndex(start-4)
            return null;
        }

        def keyBytes = new byte[keyLen]
        buffer.readBytes(keyBytes)

        return new GET(key:new String(keyBytes, "UTF-8"))
    }

    Object decodeResponse(ChannelBuffer buffer) {
        if(buffer.readableBytes() < 4) {
            return null;
        }

        def valueLen = buffer.readInt();
        if(valueLen == -1)
            return Command.RESP_NO_KEY;

        if(buffer.readableBytes() < valueLen) {
            return null;
        }

        def valueBytes = new byte[valueLen]
        buffer.readBytes(valueBytes)

        return valueBytes
    }

    void execute(Map<String,byte[]> map) {
        def value = map.get(key)

        if(value != null) {
            def buffer = new DynamicChannelBuffer(4+value.length)
            buffer.writeInt(value.length)
            buffer.writeBytes(value)
            channel.write(buffer)
        }
        else {
            def buffer = new DynamicChannelBuffer(4)
            buffer.writeInt(-1)
            channel.write(buffer)
        }
    }

    int getCode() {
        return CMD_SET
    }
}
