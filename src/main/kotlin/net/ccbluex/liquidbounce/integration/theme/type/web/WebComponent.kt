package net.ccbluex.liquidbounce.integration.theme.type.web

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

class WebComponent(
    theme: Theme,
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak> = emptyArray(),
    settings: Array<JsonObject> = emptyArray()
) : Component(theme, name, enabled, alignment, tweaks) {

    init {
        for (setting in settings) {
            configureConfigurable(this, setting)
        }
    }

}

/**
 * Assigns the value of the settings to the component
 *
 * A component can have dynamic settings which can be assigned through the JSON file
 * These have to be interpreted and assigned to the configurable
 *
 * An example:
 * {
 *     "type": "INT",
 *     "name": "Size",
 *     "default": 14,
 *     "range": {
 *         "min": 1,
 *         "max": 100
 *     },
 *     "suffix": "px"
 * }
 *
 * TOOD: Check if I can integrate this using Dynamic Configurable
 *
 * @param jsonObject JsonObject
 */
private fun configureConfigurable(configurable: Configurable, jsonObject: JsonObject) {
    val type = jsonObject["type"].asString
    val name = jsonObject["name"].asString

    // todo: might replace this with serious deserialization
    when (type) {
        "BOOLEAN" -> {
            val default = jsonObject["default"].asBoolean
            configurable.boolean(name, default)
        }
        "INT" -> {
            val default = jsonObject["default"].asInt
            val min = jsonObject["range"].asJsonObject["min"].asInt
            val max = jsonObject["range"].asJsonObject["max"].asInt
            val suffix = jsonObject["suffix"]?.asString ?: ""
            configurable.int(name, default, min..max, suffix)
        }
        "INT_RANGE" -> {
            val defaultMin = jsonObject["default"].asJsonObject["min"].asInt
            val defaultMax = jsonObject["default"].asJsonObject["max"].asInt
            val min = jsonObject["range"].asJsonObject["min"].asInt
            val max = jsonObject["range"].asJsonObject["max"].asInt
            val suffix = jsonObject["suffix"]?.asString ?: ""
            configurable.intRange(name, defaultMin..defaultMax, min..max, suffix)
        }
        "FLOAT" -> {
            val default = jsonObject["default"].asFloat
            val min = jsonObject["range"].asJsonObject["min"].asFloat
            val max = jsonObject["range"].asJsonObject["max"].asFloat
            val suffix = jsonObject["suffix"]?.asString ?: ""
            configurable.float(name, default, min..max, suffix)
        }
        "FLOAT_RANGE" -> {
            val defaultMin = jsonObject["default"].asJsonObject["min"].asFloat
            val defaultMax = jsonObject["default"].asJsonObject["max"].asFloat
            val min = jsonObject["range"].asJsonObject["min"].asFloat
            val max = jsonObject["range"].asJsonObject["max"].asFloat
            val suffix = jsonObject["suffix"]?.asString ?: ""
            configurable.floatRange(name, defaultMin..defaultMax, min..max, suffix)
        }
        "TEXT" -> {
            val default = jsonObject["default"].asString
            configurable.text(name, default)
        }
        "COLOR" -> {
            val default = jsonObject["default"].asInt
            configurable.color(name, Color4b(default, hasAlpha = true))
        }
        "CONFIGURABLE" -> {
            val configurableObject = Configurable(name)
            val settings = jsonObject["settings"].asJsonArray
            for (setting in settings) {
                configureConfigurable(configurableObject, setting.asJsonObject)
            }
            configurable.tree(configurableObject)
        }
        // same as configurable but it is [ToggleableConfigurable]
        "TOGGLEABLE" -> {
            val default = jsonObject["default"].asBoolean
            // Parent is NULL in that case because we are not dealing with Listenable anyway and only use it
            // as toggleable Configurable
            val configurableObject = object : ToggleableConfigurable(null, name, default) { }
            val settings = jsonObject["settings"].asJsonArray
            for (setting in settings) {
                configureConfigurable(configurableObject, setting.asJsonObject)
            }
            configurable.tree(configurableObject)
        }
        else -> error("Unsupported type: $type")
    }
}
