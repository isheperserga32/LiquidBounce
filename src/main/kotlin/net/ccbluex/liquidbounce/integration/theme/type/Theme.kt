package net.ccbluex.liquidbounce.integration.theme.type

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.minecraft.util.Identifier

interface Theme {
    val name: String
    val components: List<ComponentFactory>
    val wallpapers: List<Wallpaper>
    val defaultWallpaper: Wallpaper? get() = wallpapers.firstOrNull()
    val fontRenderer: FontRenderer?
        get() = null
    val textures: Map<String, Lazy<Identifier>>
        get() = hashMapOf()

    fun route(screenType: VirtualScreenType? = null): RouteType
    fun doesAccept(type: VirtualScreenType?): Boolean = doesOverlay(type) || doesSupport(type)
    fun doesSupport(type: VirtualScreenType?): Boolean
    fun doesOverlay(type: VirtualScreenType?): Boolean
    fun canSplash(): Boolean

    fun getComponentFactory(name: String): ComponentFactory? = components.firstOrNull { it.name == name }

}

sealed class RouteType(open val type: VirtualScreenType?, open val theme: Theme) {
    data class Native(
        override val type: VirtualScreenType?,
        override val theme: Theme,
        val drawableRoute: NativeDrawableRoute
    ) : RouteType(type, theme)

    data class Web(override val type: VirtualScreenType?, override val theme: Theme, val url: String) :
        RouteType(type, theme)
}
