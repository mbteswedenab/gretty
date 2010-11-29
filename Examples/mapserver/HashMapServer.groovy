@Typed package mapserver

import org.mbte.gretty.AbstractServer
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel.Channel
import org.mbte.gretty.AbstractClient
import org.jboss.netty.buffer.DynamicChannelBuffer
import groovypp.channels.ExecutingChannel
import java.util.concurrent.Executors
import org.jboss.netty.channel.ExceptionEvent
import java.util.concurrent.ConcurrentHashMap
import org.jboss.netty.buffer.HeapChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.mbte.gretty.memserver.Command
import org.mbte.gretty.memserver.MapServer
import org.mbte.gretty.memserver.SET
import org.mbte.gretty.memserver.GET
import org.mbte.gretty.memserver.MapClient
import java.nio.ByteBuffer
import groovypp.concurrent.ResourcePool
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import groovypp.concurrent.CallLaterExecutors
import org.jboss.netty.channel.ChannelFactory
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory

String [] args = binding.variables.args

if(args.length == 0) {
    println """\
Usage: mapserver [server|client]
"""
}

if(args[0] == "server") {
    new MapServer().start()
}
else {
    if(args[0] == "client") {
        def CL = 5
        def pool = CallLaterExecutors.newCachedThreadPool()
        def factory = new NioClientSocketChannelFactory(pool, pool, 32)
        ResourcePool<MapClient> clients = [
                executor: CallLaterExecutors.newCachedThreadPool(),
                initResources: { (0..<30).map{
                    connectClient(factory)
                }.asList() }
        ]

        def N = 2000000

        if(CL == 1) {
            def client = connectClient()
            def start = System.currentTimeMillis()
            for(j in 0..<N) {
                def key = "foofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoo$j"
                def value = "barbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbarbar$j"

                client.set(key, value.getBytes("UTF-8"))

                def bl = client.get(key)
                bl.get()

                assert new String((byte[])bl.get(), "UTF-8") == value

                if(j % 10000 == 0) {
                    def e = System.currentTimeMillis() - start
                    println "$j ${(j * 1000L * 2) / e} ops"
                }
            }

            def elapsed = System.currentTimeMillis() - start
            println "done in ${elapsed} ms : ${(N * 1000 * 2L) / elapsed} ops"
        }
        else {
            CountDownLatch cdl = [N]
            AtomicInteger cur = [0]
            clients.initPool()
            def start = System.currentTimeMillis()
            for(i in 0..<CL) {
                clients.execute { client ->
                    assert client

                    def j = cur.getAndIncrement ()
                    if(j >= N)
                        return

                    def key = "foo$j"
                    def value = "bar$j"

                    client.set(key, value.getBytes("UTF-8"))

                    def that = this
                    client.get(key) { bl ->
                        assert new String((byte[])bl.get(), "UTF-8") == value

                        if(j % 10000 == 0) {
                            def e = System.currentTimeMillis() - start
                            println "$j ${(j * 1000L * 2) / e} ops"
                        }

                        cdl.countDown()

                        clients.execute that
                    }
                }
            }
            cdl.await()

            def elapsed = System.currentTimeMillis() - start
            println "done in ${elapsed} ms : ${(N * 1000 * 2L) / elapsed} ops"
        }

        System.exit(0)
    }
}

private MapClient connectClient(ChannelFactory factory = null) {
    def client = new MapClient(factory)
    client.connect().await()

    if (!client.isConnected()) {
        System.err.println("Can not connect to the server")
        System.exit 0
    }
    return client
}
