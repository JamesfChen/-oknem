package com.jamesfchen.vpn

import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        val myClient = Client.createAndConnect("192.168.9.103", 8889,aioSocket = true)
//        myClient.connect("localhost", 8889)
        myClient.send(ByteBuffer.wrap("aaaaaaaaaaaaaaaaaaaaaa".toByteArray())) { respBuffer ->
           println("cjfvpn resp buffer size:${respBuffer.remaining()} ${respBuffer.toByteString().utf8()}")
        }
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

        val b = ByteBuffer.allocate(1024)
        b.put('4'.toByte())
        b.put("ff".toInt(radix = 16).toByte())
        b.put(0xfc.toByte())
        b.put(Integer.decode("0xfc").toByte())
        b.put("fc".toInt(radix = 16).toByte())
        b.put(Integer.decode("0x00").toByte())
        b.flip()
        print(b.get())
        print('\t')
//        print(b.getUByte())
//        print('\t')
//        print(b.getUShort())
//        print(b.get())
//        print(b.get())
//        print(b.getShort().toString(radix = 16))
        print('\t')
//        print(b.get())
//        print('\t')
//        print(b.get())
//        print('\t')
//        print(b.get())
        println()
        println("UInt ${UInt.MAX_VALUE} ${UInt.MIN_VALUE} ${UInt.SIZE_BYTES} ${UInt.SIZE_BITS}")
        println("Int ${Int.MAX_VALUE} ${Int.MIN_VALUE} ${Int.SIZE_BYTES} ${Int.SIZE_BITS}")
        println("UShort ${UShort.MAX_VALUE} ${UShort.MIN_VALUE} ${UShort.SIZE_BYTES} ${UShort.SIZE_BITS}")
        println("Short ${Short.MAX_VALUE} ${Short.MIN_VALUE} ${Short.SIZE_BYTES} ${Short.SIZE_BITS}")
        println("UByte ${UByte.MAX_VALUE} ${UByte.MIN_VALUE} ${UByte.SIZE_BYTES} ${UByte.SIZE_BITS}")
        println("Byte ${Byte.MAX_VALUE} ${Byte.MIN_VALUE} ${Byte.SIZE_BYTES} ${Byte.SIZE_BITS}")
        assertEquals(4, 2 + 2)
    }
}