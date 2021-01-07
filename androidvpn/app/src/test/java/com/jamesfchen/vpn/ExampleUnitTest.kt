package com.jamesfchen.vpn

import com.jamesfchen.vpn.protocol.*
import okhttp3.internal.toHexString
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    fun getPacket(file: String): Packet {
        val reader = javaClass.classLoader?.getResourceAsStream(file)?.reader()
        val ca = CharArray(2)
        val ba = ByteBuffer.allocate(BUFFER_SIZE)
        var len: Int
        while (reader?.read(ca)?.also { len = it } ?: -1 != -1) {
            val e = String(ca)
            ba.put(e.toInt(radix = 16).toByte())
        }
        ba.flip()
        return ba.getPacket()
    }
    @Test
    fun testHankshark() {
        javaClass.classLoader?.getResource("ipv4")?.file?.let {
            println(it)
            val ipv4File = File(it)
            for (file in ipv4File.listFiles()) {
                if (file.isDirectory) {
                    for (f in file.listFiles()) {
                        println(file.parentFile.name + File.separator + file.name + File.separator + f.name)
                        println(getPacket(file.parentFile.name + File.separator + file.name + File.separator + f.name))
                    }
                } else {
                    println(file.parentFile.name + File.separator + file.name)
                    println(getPacket(file.parentFile.name + File.separator + file.name))
                }
            }
//            println(getPacket("ipv4/hankshark/ack.txt"))
        }
    }

    @Test
    fun testCreatePacket() {
        // Packet(ipHeader="ipheader":{"version":V4,"ihl":20,"typeOfService":{"precedenceType":ROUTINE,"d":0,"t":0,"r":0},"totalLength":60,"identification":30029,"flags":{"df":1,"mf":0},"fragmentOffset":0,"ttl":64,"protocol":TCP,"headerChecksum":51905,"sourceAddresses":/10.0.0.2,"destinationAddresses":/59.111.181.60}, tlHeader=TcpHeader(sourcePort=41892, destinationPort=443, sequenceNo=256066452, acknowledgmentNo=0, dataOffset=40, controlBit=ControlBit(hasURG=false, hasACK=false, hasPSH=false, hasRST=false, hasSYN=true, hasFIN=false), window=65535, checksum=4570, urgentPointer=0))
        //2021-01-04 17:13:00.778 29418-29621/? D/cjfvpn/vpn_thread: dest:59.111.181.60:443
//        val p = createSynAndAckPacketOnHandshake(
//            InetSocketAddress("10.0.0.2", 780),
//            InetSocketAddress("59.111.181.60", 8080),
//            234324,
//            234234234,
//            0
//        )
////        println(p)
//        val pp = p.toByteBuffer().getPacket()
//
//        println(pp)
//        assertEquals(p.payload, pp.payload)
//        assertEquals(p.tlHeader, pp.tlHeader)
//        assertEquals(p.ipHeader, pp.ipHeader)
        println("ipv4/hankshark/sync.txt")
        val p =getPacket("ipv4/hankshark/sync.txt")
        println(p)
//            println(p.ipHeader.toByteBuffer().getIpHeader())

    }

    @Test
    fun testByteBUfferExt() {
        val b = ByteBuffer.allocate(1024)
        b.put('4'.toByte())
        b.putUByte(270.toByte())
        b.putUShort(0xffff)
        b.putUInt(0xffff_ffff)
//        b.putShort(0xffff)
        b.put(Integer.decode("0xfc").toByte())
        b.put("fc".toInt(radix = 16).toByte())
        b.put(Integer.decode("0x00").toByte())
        b.flip()

        print(b.get())
        print('\t')
        print(b.getUByte())
        print('\t')
        print(b.getUShort())
//        print(b.getShort().toString(radix = 16))
        print('\t')
        print(b.getUInt())
//        print('\t')
//        print(b.get())
//        print('\t')
//        print(b.get())

        //        val ba= byteArrayOf(0x7f,0x12,0b11111111)
//        println(
//            String.format(
//                "%d %x %o %s",
//                ba[0],
//                ba[0],
//                ba[0],
//                Integer.toBinaryString(ba[0].toInt())
//            )
//        )
        assertEquals(4, 2 + 2)
    }

    @Test
    fun addition_isCorrect() {
//        val myClient = Connection.createAndConnect("192.168.9.103", 8889, aioSocket = true)
////        myClient.connect("localhost", 8889)
//        myClient.send(ByteBuffer.wrap("aaaaaaaaaaaaaaaaaaaaaa".toByteArray())) { respBuffer ->
//            println(
//                "cjfvpn resp buffer size:${respBuffer.remaining()} ${
//                    respBuffer.toByteString().utf8()
//                }"
//            )
//        }
        val lines = javaClass.classLoader?.getResourceAsStream("ipbin.txt")?.reader()?.readLines()
            ?: listOf()
        val ba = ByteBuffer.allocate(BUFFER_SIZE)
        for (line in lines) {
            if (line == "\\n\\r\\n\\r") {
                println()
                println("read end")
                break
            }
            line.split(",").forEach { e: String ->
                print(e)
                ba.put(Integer.decode(e.trim()).toByte())
            }
        }
        ba.flip()
        println("${ba?.remaining()} ${ba.toByteString().utf8()}")
        val pip = Pipe.open()
        val selector = Selector.open()
        val sChannel: SelectableChannel = pip.source()
//        val channel: FileChannel = FileInputStream("ipbin.txt").channel
        sChannel.configureBlocking(false)
        val selectionKey = sChannel.register(selector, SelectionKey.OP_READ)
//        selectionKey.cancel()
//        while (true) {
//            val readyChannels = selector.selectNow()
//            if (readyChannels == 0) continue
//            val selectedKeys = selector.selectedKeys()
//            val iterator = selectedKeys.iterator()
//            while (iterator.hasNext()) {
//                val key = iterator.next()
//                if (key.isAcceptable) {//a connection was accepted by a ServerSocketChannel
//                } else if (key.isConnectable) {//a connection was established with a remote server
//                } else if (key.isReadable) {//a channel is ready for reading
//                } else if (key.isWritable) {//a channel is ready for writing
//                }
//                iterator.remove()
//            }
//        }

        assertEquals(4, 2 + 2)
    }
}