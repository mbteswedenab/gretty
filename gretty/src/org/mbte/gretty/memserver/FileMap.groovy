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

@Typed class FileMap {
    static class KeyDir extends AbstractConcurrentMap<byte[], byte[]> {

        Writer writer

        KeyDir (File file, Executor executor) {
            super(null)
            writer = [file]
            writer.executor = executor
        }

        public byte[] get(byte[] key) {
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

        public Entry getOrPut(byte[] key, byte[] value) {
            int hash = hash(key);
            segmentFor(hash).getOrPut(key, hash, value);
        }

        public void put(byte[] key, byte[] value) {
            int hash = hash(key);
            segmentFor(hash).put(key, hash, value);
        }

        public void remove(byte[] key) {
            int hash = hash(key);
            segmentFor(hash).remove(key, hash);
        }

        protected Segment createSegment(Object segmentInfo, int cap) {
            return new Segment(this, cap);
        }

        public static class Segment extends AbstractConcurrentMap.Segment<byte[],byte[]>{
            KeyDir keyDir

            public Segment(KeyDir keyDir, int cap) {
                super(cap)
                this.keyDir = keyDir
            }

            protected Entry createEntry(byte[] key, int hash, byte[] value) {
                return new Entry(hash: hash, segment: this, key: key, value: value)
            }
        }

        static class Entry extends AbstractQueuedLongSynchronizer implements AbstractConcurrentMap.Entry<byte[],byte[]> {
            Segment segment
            int hash

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
                    def bb = ByteBuffer.allocate(12)
                    readFully(segment.keyDir.writer.channel, bb, getState())
                    bb.flip()
                    bb.position(4)
                    def ksz = bb.getInt()
                    def vsz = bb.getInt()
                    bb = ByteBuffer.allocate(vsz)
                    readFully(segment.keyDir.writer.channel, bb, getState() + 12 + ksz)
                    value = bb.array()
                }
            }

            void setValue(byte [] value) {
                this.value = value
                setState(-1L)
                segment.keyDir.writer.write(this)
//                acquireSharedInterruptibly(0L)
//                this.value = null
            }

            void setOffset(long offset) {
                setState(offset)
                this.value = null
            }

            protected long tryAcquireShared(long arg) {
                getState() != -1L  ? 1 : -1
            }

            protected boolean tryReleaseShared(long finalState) {
                setState(finalState)
                true
            }
        }
    }

    static void readFully(FileChannel channel, ByteBuffer buffer, long from) {
        for(;;) {
            def read = channel.read(buffer, from)
            if(buffer.remaining()) {
                from += read
            }
            else
                break
        }
    }

    static void writeFully(FileChannel channel, ByteBuffer [] buffers) {
        for(int i = 0; i != buffers.length; ++i) {
            if(buffers[i].remaining())
               channel.write(buffers, i, buffers.length-i)
        }
    }

    static class Writer implements Runnable {
        private RandomAccessFile stream
        private FileChannel channel
        private ExecutorService executorService = Executors.newSingleThreadExecutor()
        private Semaphore semaphore = [0]

        protected volatile FList queue = FList.emptyList

        Executor executor

        protected static final FList busyEmptyQueue = FList.emptyList + null

        Writer (File file) {
            stream = new RandomAccessFile(file, "rw")
            channel = stream.channel

            executorService.execute(this)
        }

        void close () {
            executorService.shutdown()
            channel.force(false)
            stream.close ()
        }

        final void write(KeyDir.Entry entry) {
            for (;;) {
                def oldQueue = queue
                def newQueue = (oldQueue === busyEmptyQueue ? FList.emptyList : oldQueue)
                newQueue = newQueue + entry
                if (queue.compareAndSet(oldQueue, newQueue)) {
                    if(oldQueue.empty)
                        semaphore.release()
                    return
                }
            }
        }

        long curPos

        final void run () {
            def last = -1L
            def arr = new ByteBuffer [3]
            def bb = ByteBuffer.allocate(12)

            while(!executorService.isShutdown()) {
                def acquire = semaphore.tryAcquire(250, TimeUnit.MILLISECONDS)
                if(acquire) {
                    for (;;) {
                        def q = queue
                        if (queue.compareAndSet(q, busyEmptyQueue)) {
                            while(!q.empty) {
                                KeyDir.Entry entry = q.head
                                q = q.tail

                                bb.limit(12)
                                bb.position(0)
                                bb.putInt(entry.hash)
                                bb.putInt(entry.key.length)
                                bb.putInt(entry.value.length)
                                bb.flip()

                                arr [0] = bb
                                arr [1] = ByteBuffer.wrap(entry.key)
                                arr [2] = ByteBuffer.wrap(entry.value)

                                writeFully(channel, arr)

                                def oldPos = curPos
                                curPos = oldPos + 12 + entry.key.length + entry.value.length
                                entry.setOffset(oldPos)

                                def millis = System.currentTimeMillis()
                                if(millis - last > 1000) {
                                    last = millis
                                    channel.force(false)
                                    println '*'
                                }
                            }


                            if(!queue.compareAndSet(busyEmptyQueue, FList.emptyList)) {
                                continue
                            }
                            break
                        }
                    }
                }

                def millis = System.currentTimeMillis()
                if(millis - last > 1000) {
                    last = millis
                    channel.force(false)
                    println '*'
                }
            }
        }
    }

    public static void main(String[] args) {
        def file = File.createTempFile("aaa", "bbb")

        KeyDir keyDir = [file, Executors.newSingleThreadExecutor()]

        def start = System.currentTimeMillis()

        def executor = Executors.newFixedThreadPool(64)
        AtomicInteger counter = []
        CountDownLatch cdl = [64]
        for(k in 0..<64)
            executor.execute {
                int j
                while((j = counter.getAndIncrement()) < 1000000*10) {
                    def i = j % 1000000
                    def key   = "${i}_key_${i}_key_${i}_key_${i}_key_${i}"
                    def value   = "${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_"
                    keyDir.put(key.bytes, value.bytes)
                    def get = keyDir.get(key.bytes)
                    assert Arrays.equals(get, value.bytes)
                    if(!(j % 10000) && j) {
                        println "$j\t\t${(System.currentTimeMillis() - start) / j}"
                    }
                }
                cdl.countDown()
            }

        cdl.await()
        def duration = (System.currentTimeMillis() - start) / 1000
        println(duration)
        println file.length()/(1024L*1024*duration)

        for(j in (1000000*(10-1))..<(1000000*10)) {
            def i = j % 1000000
            def key   = "${i}_key_${i}_key_${i}_key_${i}_key_${i}"
            def value   = "${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_${j}_key_"
            def get = keyDir.get(key.bytes)
            assert Arrays.equals(get, value.bytes)
        }
//        cdl.await()
        duration = (System.currentTimeMillis() - start) / 1000
        println(duration)
//        writer.close()
//        ((ExecutorService)writer.executor).shutdown ()
        println file.length()/(1024L*1024*duration)
        println file
        file.delete()
    }
}
