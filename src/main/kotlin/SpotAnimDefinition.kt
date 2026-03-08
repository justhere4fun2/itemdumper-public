import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

data class SpotAnimDefinition(
    val id: Int,
    var modelId: Int = 0,
    var animationId: Int = -1,
    var resizeXY: Int = 128,
    var resizeZ: Int = 128,
    var rotation: Int = 0,
    var ambient: Int = 0,
    var contrast: Int = 0,
    var height: Int = 0
) {
    companion object {
        fun load(cache: CacheLibrary, spotAnimId: Int): SpotAnimDefinition {
            val def = SpotAnimDefinition(id = spotAnimId)
            val data = cache.data(2, 13, spotAnimId)
                ?: run {
                    println("[!!] SpotAnim $spotAnimId not found in cache")
                    return def
                }

            val buf = ByteBuffer.wrap(data)

            while (buf.hasRemaining()) {
                val opcode = buf.get().toInt() and 0xFF
                if (opcode == 0) break

                when (opcode) {
                    1  -> def.modelId     = buf.short.toInt() and 0xFFFF
                    2  -> def.animationId = buf.short.toInt() and 0xFFFF
                    4  -> def.resizeXY    = buf.short.toInt() and 0xFFFF
                    5  -> def.resizeZ     = buf.short.toInt() and 0xFFFF
                    6  -> def.rotation    = buf.short.toInt() and 0xFFFF
                    7  -> def.ambient     = buf.get().toInt() and 0xFF
                    8  -> def.contrast    = buf.get().toInt() and 0xFF
                    11 -> def.height      = buf.short.toInt() and 0xFFFF
                    40 -> {
                        // recolour pairs
                        val count = buf.get().toInt() and 0xFF
                        repeat(count) { buf.short; buf.short }
                    }
                    41 -> {
                        // retexture pairs
                        val count = buf.get().toInt() and 0xFF
                        repeat(count) { buf.short; buf.short }
                    }
                    else -> {
                        println("[!!] Unknown SpotAnim opcode $opcode at buffer position ${buf.position()} — stopping decode")
                        break
                    }
                }
            }
            return def
        }
    }
}