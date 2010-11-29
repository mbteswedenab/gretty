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

import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffer
import groovypp.concurrent.BindLater
import java.nio.ByteBuffer

@Typed abstract class Command<S> extends BindLater<S> {
    static final int CMD_SET = 1;
    static final int CMD_GET = 2;

    static final Object RESP_OK = []
    static final Object RESP_NO_KEY = []

    Channel channel

    abstract void execute(Map<String,byte[]> map)

    abstract ChannelBuffer encode()

    abstract Object decodeResponse(ChannelBuffer buffer)

    abstract int getCode ()

    abstract static class Decoder {
        abstract Object decode(ChannelBuffer buffer)
    }

    static final HashMap<Integer,Decoder> commandDecoders = [:]

    static {
        commandDecoders [CMD_SET] = { ch -> SET.read(ch) }
        commandDecoders [CMD_GET] = { ch -> GET.read(ch) }
    }

    static int utf8Length (String str) {
        int strLen = str.length(), utfLen = 0;
        for(int i = 0; i != strLen; ++i) {
            char c = str.charAt(i);
            if (c < 0x80) {
                utfLen++;
            } else if (c < 0x800) {
                utfLen += 2;
            } else if (isSurrogate(c)) {
                i++;
                utfLen += 4;
            } else {
                utfLen += 3;
            }
        }
        return utfLen;
    }

    private static boolean isSurrogate(char ch) {
        return ch >= Character.MIN_SURROGATE && ch <= Character.MAX_SURROGATE;
    }

    static void writeByteBuffer(ChannelBuffer buffer, ByteBuffer data) {
        buffer.writeInt(data.remaining())
        buffer.writeBytes data
    }

    static void writeUtf8(ChannelBuffer buffer, String str, int len = -1) {
        int strLen = str.length();

        if(len < 0)
            len = str.utf8Length()

        buffer.ensureWritableBytes(len+4)
        buffer.writeInt(len)

        def wi = buffer.writerIndex()

        int i;
        for (i = 0; i < strLen; i++) {
            char c = str.charAt(i)
            if (!(c < 0x80)) break;
            buffer.setByte(wi++, c);
        }

        for (; i < strLen; i++) {
            char c = str.charAt(i);
            if (c < 0x80) {
                buffer.writeByte(c);
            } else if (c < 0x800) {
                buffer.setByte(wi++, ((byte)(0xc0 | (c >> 6))))
                buffer.setByte(wi++, (byte)(0x80 | (c & 0x3f)))
            } else if (isSurrogate(c)) {
                int uc = Character.toCodePoint(c, str.charAt(i++))
                buffer.setByte(wi++, ((byte)(0xf0 | ((uc >> 18)))))
                buffer.setByte(wi++, ((byte)(0x80 | ((uc >> 12) & 0x3f))))
                buffer.setByte(wi++, ((byte)(0x80 | ((uc >> 6) & 0x3f))))
                buffer.setByte(wi++, ((byte)(0x80 | (uc & 0x3f))))
            } else {
                buffer.setByte(wi++, (byte)(0xe0 | ((c >> 12))));
                buffer.setByte(wi++, (byte)(0x80 | ((c >> 6) & 0x3f)));
                buffer.setByte(wi++, ((byte)(0x80 | (c & 0x3f))))
            }
        }
        buffer.writerIndex(wi)
    }
}
