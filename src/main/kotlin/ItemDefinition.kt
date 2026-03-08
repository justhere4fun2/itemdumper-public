import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

data class ItemDefinition(
    val id: Int,
    var name: String = "null",
    var zoom2d: Int = 2000,
    var xan2d: Int = 0,       // modelPitch  opcode 5
    var zan2d: Int = 0,       // modelRoll   opcode 6
    var yan2d: Int = 0,       // modelYaw    opcode 95
    var xoff2d: Int = 0,      // offsetX     opcode 7
    var yoff2d: Int = 0,      // offsetY     opcode 8
    var inventoryModel: Int = 0,
    var standAnim: Int = -1,
    var walkAnim: Int = -1,
    var runAnim: Int = -1,
    var walkBackAnim: Int = -1,
    var walkLeftAnim: Int = -1,
    var walkRightAnim: Int = -1,
    var maleModel0: Int = -1,   // primaryMaleModel   opcode 23
    var femaleModel0: Int = -1, // primaryFemaleModel opcode 25
    var params: MutableMap<Int, Any> = mutableMapOf()
) {
    companion object {
        fun load(cache: CacheLibrary, itemId: Int): ItemDefinition {
            val data = cache.data(2, 10, itemId)
                ?: error("No data found for item $itemId")
            return decode(itemId, data)
        }

        private fun decode(id: Int, data: ByteArray): ItemDefinition {
            val def = ItemDefinition(id)
            val buf = ByteBuffer.wrap(data)
            while (buf.hasRemaining()) {
                val opcode = buf.get().toInt() and 0xFF
                if (opcode == 0) break
                when (opcode) {
                    1   -> def.inventoryModel = buf.short.toInt() and 0xFFFF
                    2   -> def.name = readString(buf)
                    3   -> readString(buf)                                  // examine
                    4   -> def.zoom2d = buf.short.toInt() and 0xFFFF
                    5   -> def.xan2d = buf.short.toInt() and 0xFFFF        // modelPitch
                    6   -> def.zan2d = buf.short.toInt() and 0xFFFF        // modelRoll
                    7   -> def.xoff2d = buf.short.toInt()                  // offsetX
                    8   -> def.yoff2d = buf.short.toInt()                  // offsetY
                    9   -> readString(buf)
                    11  -> {}                                               // isStackable, no bytes
                    12  -> buf.int                                          // price
                    13  -> buf.get()                                        // wearPos1
                    14  -> buf.get()                                        // wearPos2
                    16  -> {}                                               // isMembers, no bytes
                    23  -> {
                        def.maleModel0 = buf.short.toInt() and 0xFFFF      // primaryMaleModel
                        buf.get()                                           // maleOffset
                    }
                    24  -> buf.short                                        // secondaryMaleModel
                    25  -> {
                        def.femaleModel0 = buf.short.toInt() and 0xFFFF    // primaryFemaleModel
                        buf.get()                                           // femaleOffset
                    }
                    26  -> buf.short                                        // secondaryFemaleModel
                    27  -> buf.get()                                        // wearPos3
                    in 30..34  -> readString(buf)                          // groundOptions
                    in 35..39  -> readString(buf)                          // inventoryOptions
                    40  -> {                                                // originalColours
                        val count = buf.get().toInt() and 0xFF
                        repeat(count) { buf.short; buf.short }
                    }
                    41  -> {                                                // originalTextures
                        val count = buf.get().toInt() and 0xFF
                        repeat(count) { buf.short; buf.short }
                    }
                    42  -> buf.get()                                        // shiftClickIndex
                    43  -> readSubOps(buf)                                  // subops
                    65  -> {}                                               // grandExchange, no bytes
                    75  -> buf.short                                        // cacheWeight
                    78  -> buf.short                                        // tertiaryMaleModel
                    79  -> buf.short                                        // tertiaryFemaleModel
                    90  -> buf.short                                        // primaryMaleHeadModel
                    91  -> buf.short                                        // primaryFemaleHeadModel
                    92  -> buf.short                                        // secondaryMaleHeadModel
                    93  -> buf.short                                        // secondaryFemaleHeadModel
                    94  -> buf.short                                        // category
                    95  -> def.yan2d = buf.short.toInt() and 0xFFFF        // modelYaw
                    97  -> buf.short                                        // notedId
                    98  -> buf.short                                        // notedTemplate
                    in 100..109 -> { buf.short; buf.short }                // stackIds/stackAmounts
                    110 -> buf.short                                        // resizeX
                    111 -> buf.short                                        // resizeY
                    112 -> buf.short                                        // resizeZ
                    113 -> buf.get()                                        // ambient
                    114 -> buf.get()                                        // contrast
                    115 -> buf.get()                                        // teamId
                    139 -> buf.short                                        // bindId
                    140 -> buf.short                                        // bindTemplateId
                    148 -> buf.short                                        // placeholderId
                    149 -> buf.short                                        // placeholderTemplate
                    249 -> def.params = readParams(buf)
                    else -> {
                        // Unknown opcode — stop decoding to avoid misreads
                        println("  WARNING: unknown opcode $opcode at position ${buf.position()} for item $id")
                        break
                    }
                }
            }
            return def
        }

        private fun readSubOps(buf: ByteBuffer) {
            buf.get() // opId — discard
            while (true) {
                val subOpId = (buf.get().toInt() and 0xFF) - 1
                if (subOpId == -1) break
                readString(buf)
            }
        }

        private fun readParams(buf: ByteBuffer): MutableMap<Int, Any> {
            val count = buf.get().toInt() and 0xFF
            val map = mutableMapOf<Int, Any>()
            repeat(count) {
                val isString = (buf.get().toInt() and 0xFF) == 1
                val key = ((buf.get().toInt() and 0xFF) shl 16) or
                        ((buf.get().toInt() and 0xFF) shl 8) or
                        (buf.get().toInt() and 0xFF)
                map[key] = if (isString) readString(buf) else buf.int
            }
            return map
        }

        private fun readString(buf: ByteBuffer): String {
            val sb = StringBuilder()
            while (buf.hasRemaining()) {
                val b = buf.get().toInt() and 0xFF
                if (b == 0 || b == 10) break
                sb.append(b.toChar())
            }
            return sb.toString()
        }
    }
}
