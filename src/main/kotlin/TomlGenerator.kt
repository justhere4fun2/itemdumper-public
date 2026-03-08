import java.io.File
import NameUtils.toSnakeCase

object TomlGenerator {

    fun generate(
        item: ItemDefinition,
        itemId: Int,
        placeholderItemId: Int,
        invModelId: Int,
        maleModelId: Int,
        femaleModelId: Int,
        inheritFrom: Int = 1704,
        automate: Boolean = true,
        outputDir: String = "models"
    ): String {
        val itemName  = item.name
        val snakeName = itemName.toSnakeCase()
        val examineText = "CHANGE THIS"

        // ── Main item block ──────────────────────────────────────────────────
        val sb = StringBuilder()
        sb.appendLine("# $itemName")
        sb.appendLine("[[item]]")
        sb.appendLine("id=${if (automate) itemId else "0 #CHANGE THIS - Set this to next available"}")
        sb.appendLine("inherit=$inheritFrom #CHANGE THIS")
        sb.appendLine("name    = \"$itemName\"")
        sb.appendLine("examine = \"$examineText\"")

        if (automate) {
            if (invModelId != 0)     sb.appendLine("invmodel=$invModelId")
            if (maleModelId != -1)   sb.appendLine("primarymalemodel=$maleModelId")
            if (femaleModelId != -1) sb.appendLine("primaryfemalemodel=$femaleModelId")
        } else {
            sb.appendLine("invmodel=0 #CHANGE THIS - register in NearRealityCustomItemPacker.kt first")
            if (item.maleModel0 != -1)   sb.appendLine("primarymalemodel=0 #CHANGE THIS - register in NearRealityCustomItemPacker.kt first")
            if (item.femaleModel0 != -1) sb.appendLine("primaryfemalemodel=0 #CHANGE THIS - register in NearRealityCustomItemPacker.kt first")
        }

        sb.appendLine("grandexchange=false")
        sb.appendLine("stackable=0")
        sb.appendLine("placeholderid=${if (automate) placeholderItemId else "0 #CHANGE THIS - set to placeholder ID"}")
        sb.appendLine("zoom=${item.zoom2d}")
        sb.appendLine("modelroll=${item.zan2d}")
        sb.appendLine("modelpitch=${item.xan2d}")
        sb.appendLine("modelyaw=${item.yan2d}")
        sb.appendLine("offsetx=${item.xoff2d}")
        sb.appendLine("offsety=${item.yoff2d}")

        // ── Placeholder block ────────────────────────────────────────────────
        sb.appendLine()
        sb.appendLine("# $itemName (Placeholder)")
        sb.appendLine("[[item]]")
        sb.appendLine("id=${if (automate) placeholderItemId else "0 #CHANGE THIS - set this to next available"}")
        sb.appendLine("inherit=$inheritFrom #CHANGE THIS")
        sb.appendLine("name=\"$itemName\"")
        sb.appendLine("placeholderid=${if (automate) itemId else "0 #CHANGE THIS - set to main item id (the itemID set for non placeholder item)"}")
        sb.appendLine("placeholdertemplate=14401")
        if (automate) {
            if (invModelId != 0) sb.appendLine("invmodel=$invModelId")
        } else {
            sb.appendLine("invmodel=0 #CHANGE THIS - Set to the same as the non placeholder item")
        }

        val tomlText = sb.toString()

        val dir = File(outputDir, snakeName)
        if (!dir.exists()) dir.mkdirs()
        val outFile = File(dir, "${snakeName}_definition.txt")
        outFile.writeText(tomlText)

        println("=== Generated TOML for item $itemId ===")
        //println(tomlText)
        println("TOML written to: ${outFile.absolutePath}")

        return tomlText
    }
}