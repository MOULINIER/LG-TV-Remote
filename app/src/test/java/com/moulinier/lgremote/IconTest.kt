package com.moulinier.lgremote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the adaptive launcher icon layers under a few launcher masks. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w400dp-h160dp-xxhdpi")
class IconTest {
    @get:Rule val compose = createComposeRule()

    @Composable
    private fun Icon(shape: Shape, themed: Boolean = false) {
        // Layers are 108dp with a 72dp visible area: draw at 1.5x the mask size (96dp mask) and clip.
        Box(
            Modifier.size(96.dp).clip(shape).background(if (themed) Color(0xFFE8DEF8) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            val layer = Modifier.wrapContentSize(unbounded = true).requiredSize(144.dp)
            if (!themed) Image(painterResource(R.drawable.ic_launcher_background), null, layer)
            Image(
                painterResource(if (themed) R.drawable.ic_launcher_monochrome else R.drawable.ic_launcher_foreground),
                null, layer,
                colorFilter = if (themed) ColorFilter.tint(Color(0xFF4A3D7A)) else null,
            )
        }
    }

    @Test fun icon() {
        compose.setContent {
            Row(Modifier.background(Color(0xFF303036)).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Icon(CircleShape)
                Icon(RoundedCornerShape(30))
                Icon(CircleShape, themed = true)
            }
        }
        compose.onRoot().captureRoboImage("../docs/icon.png")
    }
}
