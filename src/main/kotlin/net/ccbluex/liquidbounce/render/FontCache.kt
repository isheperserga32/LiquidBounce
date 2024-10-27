/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.render.engine.font.FontGlyphPageManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import java.awt.Font
import java.io.File
import kotlin.time.measureTime

object FontCache {

    internal val fontCache = mutableMapOf<String, FontHolder>()
    const val DEFAULT_FONT_SIZE: Float = 43f

    val SYSTEM_FONT
        get() = fontCache["Arial"] ?: error("System font is not loaded yet!")

    init {
        // As fallback, we can Arial. It should be available on every system. If not,
        // we should usually have other fonts available.
        queueSystemFont("Arial")
    }

    /**
     * Returns the font by the given name. If the font is not found in the cache, it will return the system font.
     */
    fun byName(name: String) = fontCache[name] ?: SYSTEM_FONT

    /**
     * TODO: Parallel task
     */
    fun workOnQueue() = measureTime {
        for ((_, reference) in fontCache) {
            reference.make()
        }
    }

    private fun queueSystemFont(name: String) {
        try {
            val fontHolder = FontHolder(name)

            arrayOf(
                Font.PLAIN,
                Font.BOLD,
                Font.ITALIC,
                Font.BOLD or Font.ITALIC
            ).map { style ->
                Font(name, style, DEFAULT_FONT_SIZE.toInt())
                    .deriveFont(DEFAULT_FONT_SIZE)
            }.forEachIndexed { index, font ->
                fontHolder.fillStyle(font, index)
            }

            fontCache[name] = fontHolder
        } catch (e: Exception) {
            logger.warn("Failed to load system font $name", e)
        }
    }

    fun queueFolder(path: File) {
        try {
            path.listFiles { file -> file.extension == "ttf" }
                ?.forEach(::queueFile)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load font from folder $path", e)
        }
    }

    fun queueFile(file: File) {
        try {
            val font = Font
                .createFont(Font.TRUETYPE_FONT, file)
                .deriveFont(DEFAULT_FONT_SIZE)

            val fontHolder = fontCache.getOrElse(font.name) { FontHolder(font.name, file) }

            // TODO: Check if the font style works
            arrayOf(
                Font.PLAIN,
                Font.BOLD,
                Font.ITALIC,
                Font.BOLD or Font.ITALIC
            ).map { style ->
                font.deriveFont(style)
            }.forEachIndexed { index, font ->
                fontHolder.fillStyle(font, index)
            }

            fontCache[font.name] = fontHolder
        } catch (e: Exception) {
            logger.warn("Failed to load font from file ${file.absolutePath}", e)
        }
    }

    data class FontHolder(
        val name: String,
        /**
         * The file of the font. If the font is a system font, this will be null.
         */
        val file: File? = null,
        @Suppress("ArrayInDataClass")
        /**
         * Style of the font. If an element is null, fall back to `[0]`
         *
         * [Font.PLAIN] -> 0 (Must not be null)
         *
         * [Font.BOLD] -> 1 (Can be null)
         *
         * [Font.ITALIC] -> 2 (Can be null)
         *
         * [Font.BOLD] | [Font.ITALIC] -> 3 (Can be null)
         */
        val fontStyles: Array<Font?> = arrayOfNulls(4)
    ) {

        private var renderer: FontRenderer? = null

        val isLoaded get() = renderer != null

        fun getRenderer() = renderer ?: error("Font was not loaded yet!")

        /**
         * Fills the font style at the given index.
         */
        fun fillStyle(style: Font, index: Int) {
            fontStyles[index] = style
        }

        /**
         * Creates a [FontRenderer] instance from the given font styles.
         */
        fun make() {
            if (isLoaded) {
                return
            }

            renderer = FontRenderer(
                fontStyles.map { it?.let { FontGlyphPageManager(it) } }.toTypedArray(),
                DEFAULT_FONT_SIZE
            )
            logger.info("Font Renderer for font $name has been loaded successfully.")
        }


    }

}
