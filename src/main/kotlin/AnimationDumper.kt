import com.displee.cache.CacheLibrary
import java.io.File
import NameUtils.toSnakeCase

object AnimationDumper {

    //private fun String.toSnakeCase() = lowercase().replace(" ", "_")

    fun dump(cache: CacheLibrary, itemId: Int, outputDir: String = "models") {
        val item = ItemDefinition.load(cache, itemId)
        val itemName = item.name.toSnakeCase()

        println("=== Animation Data for Item $itemId (${item.name}) ===")
        println("Male Model:      ${item.maleModel0.takeIf { it != -1 } ?: "none"}")
        println("Female Model:    ${item.femaleModel0.takeIf { it != -1 } ?: "none"}")
        println()

        val animations = mapOf(
            "stand"      to item.standAnim,
            "walk"       to item.walkAnim,
            "run"        to item.runAnim,
            "walk_back"  to item.walkBackAnim,
            "walk_left"  to item.walkLeftAnim,
            "walk_right" to item.walkRightAnim
        )

        val found = animations.filter { it.value != -1 }

        if (found.isEmpty()) {
            println("No worn animations defined on this item.")
            println("It likely inherits default player animations.")
            return
        }

        val dir = File(outputDir, itemName)
        if (!dir.exists()) dir.mkdirs()

        for ((label, animId) in found) {
            println("$label anim ID: $animId")
            val raw = extractRawAnimation(cache, animId)
            if (raw != null) {
                val outFile = File(dir, "${itemName}_${label}_anim${animId}.bin")
                outFile.writeBytes(raw)
                println("  → Extracted ${raw.size} bytes → ${outFile.path}")
            } else {
                println("  → Could not extract raw data for anim $animId")
            }
        }

        println()
        println("Done. Files written to: ${File(outputDir, itemName).absolutePath}")
    }

    fun dumpById(cache: CacheLibrary, outputDir: String = "animations", vararg animIds: Int) {
        val dir = File(outputDir)
        if (!dir.exists()) dir.mkdirs()

        println("=== Extracting ${animIds.size} animation(s) by ID ===")
        for (animId in animIds) {
            val raw = extractRawAnimation(cache, animId)
            if (raw != null) {
                val outFile = File(dir, "anim_${animId}.bin")
                outFile.writeBytes(raw)
                println("Anim $animId → ${raw.size} bytes → ${outFile.path}")
            } else {
                println("Anim $animId → NOT FOUND in cache")
            }
        }
        println()
        println("Done. Files written to: ${dir.absolutePath}")
    }

    fun dumpModels(cache: CacheLibrary, outputDir: String = "models", vararg modelIds: Int) {
        val dir = File(outputDir)
        if (!dir.exists()) dir.mkdirs()

        println("=== Extracting ${modelIds.size} model(s) ===")
        for (modelId in modelIds) {
            val data = cache.data(7, modelId, 0)
            if (data != null) {
                val outFile = File(dir, "model_$modelId.dat")
                outFile.writeBytes(data)
                println("Model $modelId → ${data.size} bytes → ${outFile.path}")
            } else {
                println("Model $modelId → NOT FOUND in cache")
            }
        }
        println()
        println("Done. Files written to: ${dir.absolutePath}")
    }

    fun dumpItemModels(cache: CacheLibrary, itemId: Int, outputDir: String = "models"): Map<String, File> {
        val item = ItemDefinition.load(cache, itemId)
        val itemName = item.name.toSnakeCase()

        println("=== Extracting models for item $itemId (${item.name}) ===")
        println("Inventory: ${item.inventoryModel}")
        println("Male:      ${item.maleModel0.takeIf { it != -1 } ?: "none"}")
        println("Female:    ${item.femaleModel0.takeIf { it != -1 } ?: "none"}")
        println()

        val dir = File(outputDir, itemName)
        if (!dir.exists()) dir.mkdirs()

        val resultFiles = mutableMapOf<String, File>()

        // Inventory model → drop
        if (item.inventoryModel != 0) {
            val data = cache.data(7, item.inventoryModel, 0)
            val outFile = File(dir, "${itemName}_drop.dat")
            if (data != null) {
                outFile.writeBytes(data)
                println("Inventory model ${item.inventoryModel} → ${outFile.path}")
                resultFiles["drop"] = outFile
            } else {
                println("Inventory model ${item.inventoryModel} → NOT FOUND in cache")
            }
        }

        // Male model → male_equip
        if (item.maleModel0 != -1) {
            val data = cache.data(7, item.maleModel0, 0)
            val outFile = File(dir, "${itemName}_male_equip.dat")
            if (data != null) {
                outFile.writeBytes(data)
                println("Male model ${item.maleModel0} → ${outFile.path}")
                resultFiles["male_equip"] = outFile
            } else {
                println("Male model ${item.maleModel0} → NOT FOUND in cache")
            }
        }

        // Female model → female_equip
        if (item.femaleModel0 != -1) {
            val data = cache.data(7, item.femaleModel0, 0)
            val outFile = File(dir, "${itemName}_female_equip.dat")
            if (data != null) {
                outFile.writeBytes(data)
                println("Female model ${item.femaleModel0} → ${outFile.path}")
                resultFiles["female_equip"] = outFile
            } else {
                println("Female model ${item.femaleModel0} → NOT FOUND in cache")
            }
        }

        println()
        println("Done. Files written to: ${dir.absolutePath}")
        println()

        return resultFiles
    }

    fun extractRawAnimation(cache: CacheLibrary, animId: Int): ByteArray? {
        val archive = animId ushr 7
        val file    = animId and 0x7F
        return cache.data(0, archive, file)
    }
}