package com.sallemi.gemini.ui

import com.intellij.util.ui.JBUI
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JPanel

/**
 * Custom JPanel with rounded corners for chat bubbles
 */
class RoundedBubblePanel(private val cornerRadius: Int = JBUI.scale(12)) : JPanel() {

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Fill rounded rectangle with background color
        g2.color = background
        g2.fill(RoundRectangle2D.Double(
            0.0,
            0.0,
            width.toDouble(),
            height.toDouble(),
            cornerRadius.toDouble(),
            cornerRadius.toDouble()
        ))

        g2.dispose()
        super.paintComponent(g)
    }
}
