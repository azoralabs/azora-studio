package dev.azora.studio.content_browser

/**
 * Heuristic used by the Content Browser to decide whether a file can be opened
 * as plain text in the built-in text editor.
 *
 * Anything whose extension isn't a known binary format is treated as text, so
 * that unknown or project-specific extensions (e.g. `.azn`, `.toml`) still open.
 */
private val BINARY_EXTENSIONS = setOf(
    // Images
    "png", "jpg", "jpeg", "gif", "bmp", "webp", "ico", "icns", "tiff", "tif", "heic",
    // Fonts
    "ttf", "otf", "woff", "woff2", "eot",
    // Audio
    "mp3", "wav", "ogg", "flac", "aac", "m4a",
    // Video
    "mp4", "mov", "avi", "webm", "mkv", "m4v",
    // Documents / archives
    "pdf", "zip", "jar", "war", "aar", "gz", "tar", "7z", "rar",
    // Compiled / native
    "class", "dex", "so", "dll", "dylib", "exe", "o", "a", "bin", "dat",
    // Databases
    "db", "sqlite", "sqlite3", "kdb", "kdbx"
)

fun isProbablyTextFile(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext.isNotEmpty() && ext !in BINARY_EXTENSIONS
}
