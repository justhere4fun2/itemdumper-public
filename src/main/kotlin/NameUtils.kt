object NameUtils {
    fun String.toSnakeCase(): String = this
        .lowercase()
        .replace("'", "")       // strip apostrophes
        .replace("-", "_")      // hyphens to underscores
        .replace(" ", "_")      // spaces to underscores
        .replace(Regex("[^a-z0-9_]"), "")  // strip any other special chars
}