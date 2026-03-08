import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

object ModelAnimationScanner {

    data class SequenceResult(
        val seqId: Int,
        val frameIds: List<Int>,
        val modelIds: List<Int>
    )

    data class NpcResult(
        val npcId: Int,
        val name: String,
        val modelIds: List<Int>,
        val idleAnim: Int,
        val walkAnim: Int,
        val animIds: List<Int>
    )

    fun scanForModel(cache: CacheLibrary, targetModelId: Int) {
        println("=== Scanning for model ID $targetModelId ===")
        println()

        println("--- Scanning NPC definitions (index 2, archive 9) ---")
        scanNpcs(cache, targetModelId)

        println()
        println("--- Scanning Sequence definitions (index 2, archive 12) ---")
        scanSequences(cache, targetModelId)
    }

    private fun scanNpcs(cache: CacheLibrary, targetModelId: Int) {
        val archive = cache.index(2)?.archive(9) ?: run {
            println("Could not open NPC archive")
            return
        }

        var matchCount = 0
        var errorCount = 0

        for (fileId in archive.fileIds()) {
            val data = archive.file(fileId)?.data ?: continue
            try {
                val result = decodeNpc(fileId, data)
                if (targetModelId in result.modelIds) {
                    matchCount++
                    println("NPC ${result.npcId} (${result.name})")
                    println("  Models:     ${result.modelIds}")
                    println("  IdleAnim:   ${result.idleAnim.takeIf { it != -1 } ?: "none"}")
                    println("  WalkAnim:   ${result.walkAnim.takeIf { it != -1 } ?: "none"}")
                    println("  OtherAnims: ${result.animIds}")
                    println()
                }
            } catch (e: Exception) {
                errorCount++
            }
        }

        if (matchCount == 0) println("No NPCs found referencing model $targetModelId")
        else println("Found $matchCount NPC(s) referencing model $targetModelId")
        if (errorCount > 0) println("($errorCount NPC definitions skipped due to decode errors)")
    }

    private fun scanSequences(cache: CacheLibrary, targetModelId: Int) {
        val archive = cache.index(2)?.archive(12) ?: run {
            println("Could not open Sequence archive")
            return
        }

        var matchCount = 0
        var errorCount = 0

        for (fileId in archive.fileIds()) {
            val data = archive.file(fileId)?.data ?: continue
            try {
                val result = decodeSequence(fileId, data)
                if (targetModelId in result.modelIds) {
                    matchCount++
                    println("Sequence ${result.seqId}")
                    println("  Frame IDs:  ${result.frameIds.take(8)}${if (result.frameIds.size > 8) "..." else ""}")
                    println("  Model Refs: ${result.modelIds}")
                    println()
                }
            } catch (e: Exception) {
                errorCount++
            }
        }

        if (matchCount == 0) println("No sequences found referencing model $targetModelId")
        else println("Found $matchCount sequence(s) referencing model $targetModelId")
        if (errorCount > 0) println("($errorCount sequence definitions skipped due to decode errors)")
    }

    private fun decodeNpc(id: Int, data: ByteArray): NpcResult {
        val buf = ByteBuffer.wrap(data)
        var name = "null"
        val modelIds = mutableListOf<Int>()
        var idleAnim = -1
        var walkAnim = -1
        val animIds = mutableListOf<Int>()

        while (buf.hasRemaining()) {
            val opcode = buf.get().toInt() and 0xFF
            if (opcode == 0) break
            when (opcode) {
                1 -> {
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) { modelIds.add(buf.short.toInt() and 0xFFFF) }
                }
                2          -> name = readString(buf)
                12         -> buf.get()
                13         -> {
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) { buf.short }
                }
                18         -> buf.short
                40, 41     -> {
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) { buf.short; buf.short }
                }
                43         -> walkAnim = buf.short.toInt() and 0xFFFF
                44         -> animIds.add(buf.short.toInt() and 0xFFFF)
                45         -> animIds.add(buf.short.toInt() and 0xFFFF)
                46         -> animIds.add(buf.short.toInt() and 0xFFFF)
                74         -> animIds.add(buf.short.toInt() and 0xFFFF)
                75         -> animIds.add(buf.short.toInt() and 0xFFFF)
                93         -> idleAnim = buf.short.toInt() and 0xFFFF
                // single byte opcodes
                in 30..39  -> readString(buf)
                95         -> buf.short
                97         -> buf.short
                98         -> buf.short
                99         -> buf.get()
                100        -> buf.short
                101        -> buf.get()
                102        -> buf.short
                103        -> buf.short
                107        -> buf.short
                109        -> buf.short
                111        -> buf.get()
                114        -> buf.short
                115        -> buf.get()
                116        -> buf.short
                // variable length
                106, 118   -> {
                    buf.short; buf.short
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) { buf.short }
                }
                249        -> {
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) {
                        val isString = (buf.get().toInt() and 0xFF) == 1
                        buf.get(); buf.get(); buf.get() // 3-byte key
                        if (isString) readString(buf) else buf.int
                    }
                }
                // unknown — break instead of corrupting the read position
                else       -> break
            }
        }
        return NpcResult(id, name, modelIds, idleAnim, walkAnim, animIds)
    }

    private fun decodeSequence(id: Int, data: ByteArray): SequenceResult {
        val buf = ByteBuffer.wrap(data)
        val frameIds = mutableListOf<Int>()
        val modelIds = mutableListOf<Int>()

        while (buf.hasRemaining()) {
            val opcode = buf.get().toInt() and 0xFF
            if (opcode == 0) break
            when (opcode) {
                1 -> {
                    val count = buf.short.toInt() and 0xFFFF
                    repeat(count) { frameIds.add(buf.int) }
                }
                2  -> buf.short
                3  -> {
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) { buf.get() }
                }
                4  -> {}
                5  -> buf.get()
                6  -> modelIds.add(buf.short.toInt() and 0xFFFF)
                7  -> buf.get()
                8  -> {
                    val count = buf.get().toInt() and 0xFF
                    repeat(count) { buf.short }
                }
                9  -> buf.get()
                10 -> buf.short
                11 -> buf.get()
                12 -> buf.int
                13 -> {
                    val count = buf.short.toInt() and 0xFFFF
                    repeat(count) { buf.int }
                }
                14 -> buf.int
                15 -> modelIds.add(buf.short.toInt() and 0xFFFF)
                16 -> buf.short
                else -> break
            }
        }
        return SequenceResult(id, frameIds, modelIds)
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