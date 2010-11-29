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
package org.mbte.gretty.httpserver

import org.jboss.netty.handler.stream.ChunkedStream
import org.jboss.netty.handler.codec.http.DefaultHttpChunkTrailer
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http.DefaultHttpChunk

@Typed class HttpChunkedStream extends ChunkedStream {
    private boolean lastSent

    HttpChunkedStream(InputStream is) {
        super(is)
    }

    boolean hasNextChunk() {
        super.hasNextChunk() || !lastSent
    }

    Object nextChunk() {
        if(!hasNextChunk()) {
            if(!lastSent) {
                lastSent = true
                new DefaultHttpChunkTrailer()
            }
            else {
                throw new IllegalStateException()
            }
        }
        else {
            ChannelBuffer content = super.nextChunk()
            new DefaultHttpChunk(content)
        }
    }
}
