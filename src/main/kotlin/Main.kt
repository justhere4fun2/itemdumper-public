import com.displee.cache.CacheLibrary

// ── External cache path ──────────────────────────────────────────────────────
const val EXTERNAL_CACHE_PATH =  "CHANGEME"

// ── Server cache path ──────────────────────────────────────────────────────
const val SERVER_ROOT_FOLDER = "CHANGEME"
const val SERVER_CACHE_FOLDER = "CHANGEME"

// ── Server source paths ──────────────────────────────────────────────────────
const val MODELS_OSNR_PATH      = "$SERVER_CACHE_FOLDER\\assets\\osnr\\custom_items\\models"
const val ITEM_DEFINITIONS_JSON = "$SERVER_ROOT_FOLDER\\data\\items\\ItemDefinitions.json"
const val DEFINITIONS_TOML      = "$SERVER_CACHE_FOLDER\\assets\\osnr\\custom_items\\item_config\\definitions.toml"
const val CUSTOM_ITEM_PACKER    = "$SERVER_CACHE_FOLDER\\src\\main\\kotlin\\com\\near_reality\\cache_tool\\packing\\custom\\NearRealityCustomItemPacker.kt"

fun main(args: Array<String>) {

    // Spotanim lookup, WIP
    val spotAnimIndex = args.indexOf("--spotanim")
    if (spotAnimIndex != -1) {
        val spotAnimId = args.getOrNull(spotAnimIndex + 1)?.toIntOrNull()
            ?: run {
                println("Usage: ./gradlew item-dump --args=\"--spotanim <id>\"")
                return
            }
        val cache = CacheLibrary.create(EXTERNAL_CACHE_PATH)
        val spotAnim = SpotAnimDefinition.load(cache, spotAnimId)
        println("=== SpotAnim $spotAnimId ===")
        println("Model ID:     ${spotAnim.modelId}")
        println("Animation ID: ${spotAnim.animationId.takeIf { it != -1 } ?: "none"}")
        println("Height:       ${spotAnim.height}")
        println("ResizeXY:     ${spotAnim.resizeXY}")
        println("ResizeZ:      ${spotAnim.resizeZ}")
        println("Rotation:     ${spotAnim.rotation}")
        println("Ambient:      ${spotAnim.ambient}")
        println("Contrast:     ${spotAnim.contrast}")
        println()
        println("Use in code as: new Graphics($spotAnimId, ${spotAnim.height}, 0)")
        cache.close()
        return
    }

    // Parse item args
    val itemId = args.indexOf("--itemid")
        .takeIf { it != -1 }
        ?.let { args.getOrNull(it + 1)?.toIntOrNull() }
        ?: run {
            println("Usage: ./gradlew item-dump --args=\"--itemid <id> [--automate] [--equipslot <slot>] [--inheritfrom <id>]\"")
            println("       ./gradlew item-dump --args=\"--spotanim <id>\"")
            println()
            println("Optional args:")
            println("  --automate           Copies files and patches server source automatically")
            println("  --equipslot <slot>   Equipment slot for itemdefinitions.json (default: -1)")
            println("  --inheritfrom <id>   Inherit ID for definitions.toml (default: 1704)")
            println()
            println("Equipment slots:")
            println("  0=Head  1=Cape  2=Neck  3=Weapon  4=Body  5=Shield")
            println("  7=Legs  9=Hands  10=Feet  12=Ring  13=Ammo  -1=Not equippable")
            return
        }

    val automate    = args.contains("--automate")
    val equipSlot   = args.indexOf("--equipslot")
        .takeIf { it != -1 }
        ?.let { idx ->
            val raw = args.getOrNull(idx + 1)?.lowercase() ?: return@let -1
            when (raw) {
                "0", "helm", "head", "hat", "helmet"          -> 0
                "1", "cape", "back", "backpack"                -> 1
                "2", "neck", "amulet", "ammy", "necklace"     -> 2
                "3", "weapon", "mainhand"                      -> 3
                "4", "chest", "body", "top", "shirt"           -> 4
                "5", "shield", "offhand", "book"               -> 5
                "7", "legs", "pants"                           -> 7
                "9", "hands", "gloves", "wrist"                -> 9
                "10", "feet", "boots", "shoes"                 -> 10
                "12", "ring"                                   -> 12
                "13", "ammo", "quiver"                         -> 13
                "none", "-1"                                   -> -1
                else -> {
                    println("[!!] Unknown equipslot value '$raw', defaulting to -1")
                    println("     Valid strings: helm, cape, neck, weapon, chest, shield, legs, hands, feet, ring, ammo, none")
                    -1
                }
            }
        } ?: -1
    val inheritFrom = args.indexOf("--inheritfrom")
        .takeIf { it != -1 }
        ?.let { args.getOrNull(it + 1)?.toIntOrNull() }
        ?: 1704

    // Load Cache
    val cache = CacheLibrary.create(EXTERNAL_CACHE_PATH)
    val item  = ItemDefinition.load(cache, itemId)

    // Print Display Data
    println("=== Item Display Data ===")
    println("Name:         ${item.name}")
    println("Model:        ${item.inventoryModel}")
    println("Zoom:         ${item.zoom2d}")
    println("Pitch:        ${item.xan2d}")
    println("Roll:         ${item.zan2d}")
    println("Yaw:          ${item.yan2d}")
    println("XOff:         ${item.xoff2d}")
    println("YOff:         ${item.yoff2d}")
    println("Male Model:   ${item.maleModel0.takeIf { it != -1 } ?: "none"}")
    println("Female Model: ${item.femaleModel0.takeIf { it != -1 } ?: "none"}")
    println()

    // Dump models locally
    val localModelPaths = AnimationDumper.dumpItemModels(cache, itemId)
    AnimationDumper.dump(cache, itemId)

    if (automate) {
        AutomationHandler.run(
            item            = item,
            itemId          = itemId,
            localModelPaths = localModelPaths,
            equipSlot       = equipSlot,
            inheritFrom     = inheritFrom
        )
    } else {
        TomlGenerator.generate(
            item              = item,
            itemId            = itemId,
            placeholderItemId = 0,
            invModelId        = item.inventoryModel,
            maleModelId       = item.maleModel0,
            femaleModelId     = item.femaleModel0,
            inheritFrom       = inheritFrom,
            automate          = false
        )
    }

    cache.close()
}