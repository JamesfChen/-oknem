package com.jamesfchen.vpn

import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testByteBUfferExt(){
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
        assertEquals(4, 2 + 2)
    }
    @Test
    fun createPacket() {
        // Packet(ipHeader="ipheader":{"version":V4,"ihl":20,"typeOfService":{"precedenceType":ROUTINE,"d":0,"t":0,"r":0},"totalLength":60,"identification":30029,"flags":{"df":1,"mf":0},"fragmentOffset":0,"ttl":64,"protocol":TCP,"headerChecksum":51905,"sourceAddresses":/10.0.0.2,"destinationAddresses":/59.111.181.60}, tlHeader=TcpHeader(sourcePort=41892, destinationPort=443, sequenceNo=256066452, acknowledgmentNo=0, dataOffset=40, controlBit=ControlBit(hasURG=false, hasACK=false, hasPSH=false, hasRST=false, hasSYN=true, hasFIN=false), window=65535, checksum=4570, urgentPointer=0))
        //2021-01-04 17:13:00.778 29418-29621/? D/cjfvpn/vpn_thread: dest:59.111.181.60:443
        val ipHeader = IpHeader(
            IpVersion.V4,20, TypeOfService(0),totalLength = 60,
                    identification=30029,flags = IpFlag(0b010),fragmentOffset = 0,ttl = 64,protocol = Protocol.TCP,headerChecksum = 51905,sourceAddresses= InetAddress.getByName("10.0.0.2"),destinationAddresses = InetAddress.getByName("59.111.181.60")
        )
        var tlHeader:TransportLayerHeader?=null
        if (ipHeader.protocol ==Protocol.TCP){
             tlHeader =TcpHeader(sourcePort = 41892,destinationPort = 443,sequenceNo = 256066452,acknowledgmentNo = 0,dataOffset = 40,controlBit = ControlBit(0b000010),window = 65535,checksum = 4570,urgentPointer = 0)
            tlHeader.optionsAndPadding=ByteArray(20)

        }
        val p= Packet(ipHeader,tlHeader,ByteBuffer.wrap(byteArrayOf()))
        println(p)
        val pp = p.toByteBuffer().getPacket()

        println(pp)
        assertEquals(p.payload,pp.payload)
        assertEquals(p.tlHeader,pp.tlHeader)
        assertEquals(p.ipHeader,pp.ipHeader)

    }

    @Test
    fun addition_isCorrect() {
        val myClient = Client.createAndConnect("192.168.9.103", 8889, aioSocket = true)
//        myClient.connect("localhost", 8889)
        myClient.send(ByteBuffer.wrap("aaaaaaaaaaaaaaaaaaaaaa".toByteArray())) { respBuffer ->
            println(
                "cjfvpn resp buffer size:${respBuffer.remaining()} ${
                    respBuffer.toByteString().utf8()
                }"
            )
        }
        Byte.MAX_VALUE
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
//        println("ff".toInt(radix=16))

        println()
        assertEquals(4, 2 + 2)
    }
}