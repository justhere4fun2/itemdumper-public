import java.io.File
import NameUtils.toSnakeCase

object AutomationHandler {

    fun run(
        item: ItemDefinition,
        itemId: Int,
        localModelPaths: Map<String, File>,
        equipSlot: Int = -1,
        inheritFrom: Int = 1704
    ) {
        val snakeName = item.name.toSnakeCase()
        val changes = mutableListOf<String>()

        println("=== Running Automate Mode ===")
        println()

        // ── Step 1: Copy .dat files to both asset folders ────────────────────
        val assetDirs = listOf(
            File(MODELS_OSNR_PATH)
        )
        for ((label, srcFile) in localModelPaths) {
            if (!srcFile.exists()) {
                changes.add("[!!] SKIPPED copy for $label -- file not found: ${srcFile.path}")
                continue
            }
            for (dir in assetDirs) {
                val dest = File(dir, srcFile.name)
                srcFile.copyTo(dest, overwrite = true)
                changes.add("[OK] Copied ${srcFile.name} -> ${dest.absolutePath}")
            }
        }

        // ── Step 2: Scan packer and assign new model IDs ─────────────────────
        val packerFile = File(CUSTOM_ITEM_PACKER)
        val packerLines = packerFile.readLines().toMutableList()

        val modelIdRegex = Regex("""modelId\s*=\s*(\d+)""")
        val lastModelId = packerLines
            .mapNotNull { modelIdRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
            ?: run {
                println("[!!] Could not find last modelId in packer file. Aborting automate.")
                return
            }

        val dropModelId = lastModelId + 1
        var nextId = dropModelId + 1
        val femaleModelId = if (item.femaleModel0 != -1) nextId++ else -1
        val maleModelId   = if (item.maleModel0 != -1) nextId else -1

        val insertIndex = packerLines.indexOfLast { it.contains("to CustomDefinition.Model(modelId =") }
        if (insertIndex == -1) {
            println("[!!] Could not find insertion point in packer file. Aborting automate.")
            return
        }

        val newPackerLines = mutableListOf<String>()
        newPackerLines.add("\t\t\"${snakeName}_drop\"         to CustomDefinition.Model(modelId = $dropModelId),")
        if (femaleModelId != -1) {
            newPackerLines.add("\t\t\"${snakeName}_female_equip\" to CustomDefinition.Model(modelId = $femaleModelId),")
        }
        if (maleModelId != -1) {
            newPackerLines.add("\t\t\"${snakeName}_male_equip\"   to CustomDefinition.Model(modelId = $maleModelId),")
        }

        packerLines.addAll(insertIndex + 1, newPackerLines)
        packerFile.writeText(packerLines.joinToString("\n"))

        changes.add("[OK] NearRealityCustomItemPacker.kt - added model entries:")
        newPackerLines.forEach { changes.add("     $it") }

        // ── Step 3: Patch definitions.toml ───────────────────────────────────
        val tomlFile = File(DEFINITIONS_TOML)
        var tomlContent = tomlFile.readText()

        val nextIdRegex = Regex("""## NEXT ID STARTS FROM (\d+)""")
        val nextIdMatch = nextIdRegex.find(tomlContent)
            ?: run {
                println("[!!] Could not find NEXT ID comment in definitions.toml. Aborting automate.")
                return
            }
        val newItemId = nextIdMatch.groupValues[1].toInt()
        val newPlaceholderItemId = newItemId + 1
        val updatedNextId = newItemId + 2  // reserve 2 IDs: item + placeholder

        val tomlBlock = TomlGenerator.generate(
            item              = item,
            itemId            = newItemId,
            placeholderItemId = newPlaceholderItemId,
            invModelId        = dropModelId,
            maleModelId       = maleModelId,
            femaleModelId     = femaleModelId,
            inheritFrom       = inheritFrom,
            automate          = true
        )

        tomlContent = tomlContent.replace(
            nextIdMatch.value,
            "$tomlBlock\n${nextIdMatch.value.replace(newItemId.toString(), updatedNextId.toString())}"
        )
        tomlFile.writeText(tomlContent)

        changes.add("[OK] definitions.toml - appended item $newItemId (${item.name})")
        changes.add("[OK] definitions.toml - appended placeholder item $newPlaceholderItemId")
        changes.add("[OK] definitions.toml - updated NEXT ID comment to $updatedNextId")

        // ── Step 4: Patch itemdefinitions.json ───────────────────────────────
        val jsonFile = File(ITEM_DEFINITIONS_JSON)
        var jsonContent = jsonFile.readText()

        val newJsonEntry = """    {
        "id": $newItemId,
        "weight": 0,
        "slot": $equipSlot,
        "equipmentType": "DEFAULT"
    }"""

        val lastBrace = jsonContent.lastIndexOf('}')
        if (lastBrace == -1) {
            println("[!!] Could not find insertion point in itemdefinitions.json. Aborting automate.")
            return
        }
        jsonContent = jsonContent.substring(0, lastBrace + 1) +
                ",\n$newJsonEntry" +
                jsonContent.substring(lastBrace + 1)
        jsonFile.writeText(jsonContent)

        changes.add("[OK] itemdefinitions.json - added entry for item $newItemId (slot=$equipSlot)")

        // ── Summary ──────────────────────────────────────────────────────────
        println()
        println("=== Automate Summary ===")
        changes.forEach { println(it) }
        println()
        println("[!!] Manual steps still required:")
        println("  1. definitions.toml     - update examine= for item $newItemId")
        if (inheritFrom == 1704) {
            println("  2. definitions.toml     - update inherit= for item $newItemId and placeholder $newPlaceholderItemId")
        }
        if (equipSlot == -1) {
            println("  3. itemdefinitions.json - update slot= for item $newItemId (currently -1)")
        }
        println("  4. itemdefinitions.json - update weight= for item $newItemId")
        println("  5. Run: ./gradlew generatecacheprod")
        println("  6. Run: ./gradlew runproduction")
    }
}