package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.web.WebComponent
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class ComponentFactory {

    abstract val name: String
    abstract val default: Boolean

    class JsonComponentFactory(
        override val name: String,
        override val default: Boolean,
        private val alignment: Alignment,
        private val tweaks: Array<ComponentTweak>?,
        private val settings: Array<JsonObject>?,
    ) : ComponentFactory() {
        override fun new(theme: Theme) =
            WebComponent(
                theme,
                name,
                true,
                alignment,
                tweaks ?: emptyArray(),
                settings ?: emptyArray()
            )
    }

    class NativeComponentFactory(
        override val name: String,
        override val default: Boolean = false,
        private val function: () -> Component,
    ) : ComponentFactory() {
        override fun new(theme: Theme) = function()
    }

    abstract fun new(theme: Theme): Component

}
