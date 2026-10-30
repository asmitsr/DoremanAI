package com.sneha.ai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.delay

/**
 * v2.1 — 3D mascot for the home screen.
 *
 * Loads "models/doreman.glb" from assets with SceneView 2.1.0 (Filament) and
 * plays the GLB animation matching the assistant state. This is NOT the old
 * Canvas Orb: when the model loads, the 3D model is what the user sees.
 *
 * Fallback is deliberate and silent: if the model cannot be created (missing
 * asset, unsupported device, SceneView error), the existing 2D Orb() is shown
 * instead, so the home screen never crashes because of the mascot.
 *
 * @param happyTrigger increment this counter when an action completes
 *   successfully; the happy animation plays once (~2s) and returns to base.
 * @param touchTrigger increment this counter on mascot tap; the touch
 *   animation plays once (~1.5s) and returns to base. The caller still
 *   handles the tap itself (microphone), this only animates it.
 * @param reduceMotion when true, the model is shown as a static frame:
 *   no looping or transient animations are played.
 */
@Composable
fun Mascot3D(
    phase: Phase,
    level: Float,
    happyTrigger: Int,
    touchTrigger: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    // level is accepted for API compatibility with Orb(phase, level, ...).
    // The 3D speaking animation already moves the mouth, so no extra
    // voice-level scaling is applied to the model here.
    @Suppress("UNUSED_PARAMETER")
    val unusedLevel = level

    var modelFailed by remember { mutableStateOf(false) }
    var transient by remember { mutableStateOf(MascotTransient.NONE) }

    LaunchedEffect(happyTrigger) {
        if (happyTrigger > 0 && !reduceMotion) {
            transient = MascotTransient.HAPPY
            delay(MascotAnimations.HAPPY_MS)
            transient = MascotTransient.NONE
        }
    }
    LaunchedEffect(touchTrigger) {
        if (touchTrigger > 0 && !reduceMotion) {
            transient = MascotTransient.TOUCH
            delay(MascotAnimations.TOUCH_MS)
            transient = MascotTransient.NONE
        }
    }

    if (modelFailed) {
        Orb(phase = phase, level = level, reduceMotion = reduceMotion, modifier = modifier)
        return
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine).apply { position = Position(z = 4.0f) }
    val childNodes = rememberNodes()
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }

    // Load once. createModelInstance throws on a missing/corrupt asset, so
    // the failure is caught here and routed to the Orb fallback above.
    LaunchedEffect(Unit) {
        try {
            val node = ModelNode(
                modelInstance = modelLoader.createModelInstance("models/doreman.glb"),
                scaleToUnits = 1.0f
            )
            modelNode = node
            childNodes.add(node)
        } catch (_: Exception) {
            modelFailed = true
        }
    }

    val (animationName, loop) = MascotAnimations.resolve(phase, transient)
    LaunchedEffect(animationName, loop, reduceMotion, modelNode) {
        val node = modelNode ?: return@LaunchedEffect
        if (reduceMotion) return@LaunchedEffect
        try {
            MascotAnimations.ALL.forEach { node.stopAnimation(it) }
            node.playAnimation(animationName, loop)
            // Blink is layered on top of every base/transient animation:
            // ModelNode keeps one PlayingAnimation per animation index, so
            // blink (eyes only) does not conflict with head/mouth/arm tracks.
            if (animationName != MascotAnimations.BLINK) {
                node.playAnimation(MascotAnimations.BLINK, true)
            }
        } catch (_: Exception) {
            modelFailed = true
        }
    }

    Box(modifier = modifier) {
        Scene(
            modifier = Modifier.matchParentSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            childNodes = childNodes
        )
    }
}
