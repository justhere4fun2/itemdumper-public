
/*

import com.displee.cache.CacheLibrary


import java.io.File

fun main() {
    val library = CacheLibrary("C:\\Users\\A\\Downloads\\cache-oldschool-live-en-b227-2024-11-27-12-30-05-openrs2#1974\\cache")
    val modelId = 55588

    val index = library.index(7) ?: run { println("Index 7 not found"); return }
    index.cache()

    val archive = index.archive(modelId) ?: run { println("Archive $modelId not found"); return }
    val file = archive.file(0) ?: run { println("File not found"); return }

    if (file.data != null) {
        File("C:/Users/A/Desktop/leprechauns_vault.dat").writeBytes(file.data!!)
        println("Wrote leprechauns_vault.dat!")
    } else {
        println("File data is null")
    }
}
 */



import com.displee.cache.CacheLibrary

fun main() {
    val library = CacheLibrary("C:\\Users\\A\\Downloads\\cache-oldschool-live-en-b227-2024-11-27-12-30-05-openrs2#1974\\cache")

    val index = library.index(4) ?: run { println("Index 4 not found"); return }
    index.cache()

    println("Max archive id: ${index.archiveIds().max()}")
    println("Last 10 archive ids: ${index.archiveIds().takeLast(10).toList()}")
}