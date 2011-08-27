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



package org.mbte.gretty.memserver

import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import groovypp.concurrent.FList
import java.util.concurrent.Executor
import java.util.concurrent.CountDownLatch

import org.codehaus.groovy.util.AbstractConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.locks.AbstractQueuedLongSynchronizer
import java.util.concurrent.Semaphore
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import groovypp.concurrent.BindLater
import java.util.concurrent.locks.ReentrantLock
import groovypp.concurrent.FQueue
import org.mbte.gretty.memserver.FileMap.KeyDir.Entry
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import apple.laf.JRSUIConstants.Size
import java.util.concurrent.atomic.AtomicLong
import java.nio.channels.Channel

@Typed class FileMap {
    static class KeyDir extends AbstractConcurrentMap<byte[], byte[]> {
        Writer writer = []

        KeyDir (File file) {
            super(null)
            writer.open (file)
        }

        byte[] get(byte[] key) {
            int hash = hash(key);
            segmentFor(hash).get(key, hash);
        }

        protected static <K> int hash(byte[] key) {
            int h = Arrays.hashCode(key);
            h += ~(h << 9);
            h ^=  (h >>> 14);
            h +=  (h << 4);
            h ^=  (h >>> 10);
            return h;
        }

        Entry getOrPut(byte[] key, byte[] value) {
            int hash = hash(key);
            segmentFor(hash).getOrPut(key, hash, value);
        }

        void put(byte[] key, byte[] value) {
            int hash = hash(key);
            KeyDir.Segment segment = segmentFor(hash)

            def wsize = 12 + key.length + value.length
            def out = ByteBuffer.allocate(wsize)

            out.position(0)
            out.limit(wsize)

            out.putInt(hash).
                putInt(key.length).
                putInt(value.length).
                put(key).
                put(value)
            out.flip()

            def offset = writer.write(out)

            Entry e = segment.put(key, hash, value)
            e.offset = offset
            e.value = null
        }

        void remove(byte[] key) {
            int hash = hash(key);
            segmentFor(hash).remove(key, hash);
        }

        protected Segment createSegment(Object segmentInfo, int cap) {
            return new Segment(this, cap);
        }

        public static class Segment extends AbstractConcurrentMap.Segment<byte[],byte[]>{
            KeyDir keyDir

            protected ByteBuffer out = ByteBuffer.allocate(64*1024)

            public Segment(KeyDir keyDir, int cap) {
                super(cap)
                this.keyDir = keyDir
            }

            protected Entry createEntry(byte[] key, int hash, byte[] value) {
                return new Entry(hash: hash, segment: this, key: key, value: value)
            }
        }

        static class Entry implements AbstractConcurrentMap.Entry<byte[],byte[]> {
            Segment segment
            int hash, valueSize
            long offset

            byte [] key, value

            boolean isEqual(byte[] key, int hash) {
                this.hash == hash && Arrays.equals(this.key, key)
            }

            boolean isValid() {
                return true
            }

            byte [] getValue() {
                if(value != null)
                    value
                else {
                    def bb = ByteBuffer.allocate(valueSize)
                    readFully(segment.keyDir.writer.channel, bb, offset + 12 + key.length)
                    value = bb.array()
                }
            }

            void setValue(byte [] avalue) {
                this.value = avalue
                if(avalue != null)
                    valueSize = avalue.length
            }
        }
    }

    static void readFully(FileChannel channel, ByteBuffer buffer, long from) {
        while(buffer.remaining()) {
            from += channel.read(buffer, from)
        }
    }

    static void writeFully(FileChannel channel, ByteBuffer [] buffers) {
        writeFully(channel, buffers, 0, buffers.length)
    }

    static void writeFully(FileChannel channel, ByteBuffer [] buffers, int from, int to) {
        for(int i = from; i != to; ++i) {
            if(buffers[i].remaining())
               channel.write(buffers, i, to-i)
        }
    }

    static void writeFully(FileChannel channel, ByteBuffer buffer) {
        while(buffer.remaining()) {
           channel.write(buffer)
        }
    }

    abstract static class AbstractFileWriter implements Runnable {
        private RandomAccessFile stream
        private FileChannel channel
        private ExecutorService executorService = Executors.newSingleThreadExecutor()

        void open (File file) {
            stream = new RandomAccessFile(file, "rw")
            channel = stream.channel

            executorService.execute(this)
        }

        void close () {
            executorService.shutdown()
            channel.force(false)
            stream.close ()
        }

        abstract long write(ByteBuffer buffer)
    }

    static class Writer extends AbstractFileWriter implements Runnable {
        private AtomicLong pos = []
        private ReentrantReadWriteLock lock = [true]

        long write(ByteBuffer out) {
            def wsize = out.remaining()
            def offset = pos.addAndGet(wsize) - wsize
            lock.readLock().lock()
            try {
                def written = offset
                while(out.hasRemaining())
                    written += channel.write(out, written)
            }
            finally {
                lock.readLock().unlock()
            }
            offset
        }

        final void run () {
            def last = -1L

            while(!executorService.isShutdown()) {
                Thread.sleep(250)
                def millis = System.currentTimeMillis()
                if(millis - last > 1000) {
                    lock.writeLock().lock()
                    try {
                        if(channel.open)
                            channel.force(false)
                    }
                    finally {
                        lock.writeLock().unlock()
                    }
                    last = System.currentTimeMillis()
                    println '*'
                }
            }
        }
    }

    public static void main(String[] args) {
        def file = File.createTempFile("aaa", "bbb")
        println file

        KeyDir keyDir = [file]

        def start = System.currentTimeMillis()

        def threadCount = 6
        def executor = Executors.newFixedThreadPool(threadCount)
        CountDownLatch cdl = [threadCount]
        AtomicInteger counter = []
        for(k in 0..<threadCount)
            executor.execute {
                int j
                while((j = counter.getAndIncrement()) < 1000000*10) {
                    def i = j % 1000000
                    def key   = "${i}_key_${i}_key_${i}_key_${i}_key_${i}".bytes
                    def value   = "${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_".bytes
//                    def value   = "${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_".bytes
//                    def value   = "${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_".bytes
//                    def value   = new byte [64]

                    keyDir.put(key, value)
                    if(!(j % 10000) && j) {
                        println "$j\t\t${(System.currentTimeMillis() - start) / j}ms/op\t${(j * 1000L) / (System.currentTimeMillis() - start)}op/sec"
                    }
                }
                cdl.countDown()
            }

        cdl.await()
        def duration = (System.currentTimeMillis() - start) / 1000
        println "${duration}sec"
        println "${file.length()/(1024L*1024*duration)}Mb/sec"

        for(j in (1000000*(10-1))..<(1000000*10)) {
            def i = j % 1000000
            def key   = "${i}_key_${i}_key_${i}_key_${i}_key_${i}"
            def value   = "${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_"
            def get = keyDir.get(key.bytes)
            assert Arrays.equals(get, value.bytes)
            if(i % 10000 == 0)
                println i
        }
//        cdl.await()
        duration = (System.currentTimeMillis() - start) / 1000
        println(duration)
        keyDir.writer.close()
        executor.shutdown()
        println file
        file.delete()
    }
}
