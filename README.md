# NR Item Dumper

> **Before using this tool:** If you are not using version control, it is strongly recommended that you set up a git repository for your project and track your changes using Git, GitHub Desktop, or another version control tool. This tool makes direct edits to your server source files and having a backup is important.

---

## Section 1 — Setup

Open `src\main\kotlin\Main.kt` and set the following three constants at the top of the file:

```kotlin
const val EXTERNAL_CACHE_PATH = "path/to/the/cache/you/want/to/extract/from"
const val SERVER_ROOT_FOLDER  = "path/to/your/server/root"
const val SERVER_CACHE_FOLDER = "path/to/your/server/cache"
```

- `EXTERNAL_CACHE_PATH` — the cache you are extracting item data and models from
- `SERVER_ROOT_FOLDER` — the root directory of your server source
- `SERVER_CACHE_FOLDER` — the cache directory within your server source

---

## Section 2 — Usage

All commands are run from the terminal inside the project directory.

### Base command

```
./gradlew item-dump --args="--itemid <id>"
```

This will extract the item's models locally into a `models/` folder and generate a `.txt` file containing a ready-to-edit TOML definition snippet. Nothing in your server source will be touched.

### Full automate command

```
./gradlew item-dump --args="--itemid <id> --automate --equipslot <slot> --inheritfrom <id>"
```

---

### Arguments

#### `--itemid` *(required)*

The item ID from the **external cache** you wish to extract.

You can find item IDs using [Qodat](https://github.com/Z-Kris/qodat) or the OSRS Wiki item ID list:
https://oldschool.runescape.wiki/w/Item_IDs

> Make sure you are on the correct cache version when cross-referencing the wiki.

---

#### `--automate` *(optional)*

Automatically performs the following:
- Copies extracted model `.dat` files into your server's custom items model folder
- Adds model entries to `NearRealityCustomItemPacker.kt` with auto-incremented model IDs
- Appends the item and its bank placeholder entry to `definitions.toml` with auto-assigned item IDs
- Adds a placeholder entry to `ItemDefinitions.json`

> **Important:** `definitions.toml` must have the following line at the bottom of the file for ID assignment to work:
> ```
> ## NEXT ID STARTS FROM <next available id> (inclusive)
> ```
> The tool reads this line to assign the item's in-game ID and increments it automatically after each run.

After running with `--automate`, the following fields will still need to be filled in manually:
- `examine=` in `definitions.toml`
- `inherit=` in `definitions.toml` (if not provided via `--inheritfrom`)
- `slot=` and `weight=` in `ItemDefinitions.json` (if not provided via `--equipslot`)

---

#### `--equipslot` *(optional)*

Sets the equipment slot written to `ItemDefinitions.json`. Accepts either the numeric value or any of the string aliases listed below. Defaults to `-1` (not equippable) if not provided.

| Aliases | Slot |
|---------|------|
| `helm` `head` `hat` `helmet` `0` | 0 |
| `cape` `back` `backpack` `1` | 1 |
| `neck` `amulet` `ammy` `necklace` `2` | 2 |
| `weapon` `mainhand` `3` | 3 |
| `chest` `body` `top` `shirt` `4` | 4 |
| `shield` `offhand` `book` `5` | 5 |
| `legs` `pants` `7` | 7 |
| `hands` `gloves` `wrist` `9` | 9 |
| `feet` `boots` `shoes` `10` | 10 |
| `ring` `12` | 12 |
| `ammo` `quiver` `13` | 13 |
| `none` `-1` | -1 |

**Example:**
```
./gradlew item-dump --args="--itemid 30376 --automate --equipslot neck"
./gradlew item-dump --args="--itemid 30376 --automate --equipslot 2"
```

---

#### `--inheritfrom` *(optional)*

Sets the `inherit=` value written to `definitions.toml`. 

Defaults to `1704` if not provided, which will be flagged as a placeholder in the output reminding you to update it.

**Example:**
```
./gradlew item-dump --args="--itemid 30376 --automate --inheritfrom 1731"
```

---

### SpotAnim lookup

```
./gradlew item-dump --args="--spotanim <id>"
```

Looks up a SpotAnim definition from the external cache and prints its properties including the height value, which can be used when playing graphics effects on a player:

```java
new Graphics(spotAnimId, delay, height)
```

---

### Example — full command

```
./gradlew item-dump --args="--itemid 30376 --automate --equipslot neck --inheritfrom 1731"
```