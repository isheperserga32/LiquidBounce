package net.ccbluex.liquidbounce.integration.theme.type.web

data class ThemeMetadata(
    val name: String,
    val authors: List<String>,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    // If null, there is no default wallpaper for this theme assigned
    val wallpaper: String? = null
)
